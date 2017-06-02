package org.har01d.imovie.btt;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.MyThreadFactory;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Source;
import org.har01d.imovie.service.MovieService;
import org.har01d.imovie.util.HttpUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BttCrawlerImpl implements BttCrawler {

    private static final Logger logger = LoggerFactory.getLogger(BttCrawler.class);
    private static final Pattern SUBJECT_PATTERN = Pattern
        .compile("^\\[([^]]+)] \\[[^]]+] \\[([^]]+)] \\[([^]]+)]\\[([^]]+)]\\[[^]]+]\\[[^]]+].*$");

    private static final Pattern SUBJECT_PATTERN2 = Pattern
        .compile("^\\[([^]]+)] \\[[^]]+] \\[([^]]+)] \\[[^]]+](.*)$");

    @Value("${url.btt.page}")
    private String baseUrl;

    @Value("${url.btt.site}")
    private String siteUrl;

    @Autowired
    private BttParser parser;

    @Autowired
    private MovieService service;

    @Override
    public void crawler() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(4, new MyThreadFactory("BttCrawler"));
        executorService.submit(() -> work(950));
        executorService.submit(() -> work(951));
//        executorService.submit(() -> work(981));
        executorService.submit(() -> work(1183));
        executorService.submit(() -> work(1193));
        executorService.shutdown();
    }

    private void work(int fid) {
        int total = 0;
        int page = getPage(fid);
        Config full = service.getConfig("btt_crawler_" + fid);
        while (true) {
            String url = String.format(baseUrl, fid, page);
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                String date = doc.select("td.username .small").last().text();
                Integer year = service.getYear(date);
                if (year != null && year <= 2012) {
                    full = service.saveConfig("btt_crawler_" + fid, "full");
                    page = 1;
                    continue;
                }
                Elements elements = doc.select("td.subject");

                int count = 0;
                for (Element element : elements) {
                    String text = element.text();
                    Movie movie = null;
                    String pageUrl = null;
                    Matcher matcher = SUBJECT_PATTERN.matcher(text);
                    if (matcher.find()) {
                        String str = matcher.group(3);
                        String name = str;
                        if (str.contains("BT") || str.contains("下载") || str.contains("网盘")) {
                            name = matcher.group(4);
                        }
                        pageUrl = siteUrl + element.select("a").attr("href");
                        logger.info(fid + "-" + page + "-" + total + "-" + count + " " + name + ": " + pageUrl);
                        if (service.findSource(pageUrl) != null) {
                            continue;
                        }

                        String y = matcher.group(1);
                        movie = new Movie();
                        movie.setTitle(text);
                        movie.setName(getName(name));
                        if (y.matches("\\d{4}")) {
                            movie.setYear(Integer.valueOf(y));
                        }
                        movie.setCategories(service.getCategories(Collections.singleton(matcher.group(2))));
                    } else {
                        matcher = SUBJECT_PATTERN2.matcher(text);
                        if (matcher.find()) {
                            pageUrl = siteUrl + element.select("a").attr("href");
                            logger.info(
                                fid + "-" + page + "-" + total + "-" + count + " " + matcher.group(3) + ": " + pageUrl);
                            if (service.findSource(pageUrl) != null) {
                                continue;
                            }

                            String y = matcher.group(1);
                            movie = new Movie();
                            movie.setTitle(text);
                            if (y.matches("\\d{4}")) {
                                movie.setYear(Integer.valueOf(y));
                            }
                            movie.setCategories(service.getCategories(Collections.singleton(matcher.group(2))));
                        }
                    }

                    if (movie == null) {
                        continue;
                    }

                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            service.save(new Source(pageUrl));
                            count++;
                            total++;
                        }
                    } catch (Exception e) {
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("Parse page failed: " + pageUrl, e);
                    }
                }

                if (full != null && count == 0) {
                    break;
                }
                page++;
                savePage(fid, page);
            } catch (IOException e) {
                service.publishEvent(url, e.getMessage());
                logger.error("Get HTML failed: " + url, e);
            }
        }

        savePage(fid, 1);
        logger.info("===== get {} movies =====", total);
    }

    private String getName(String title) {
        String[] comps = title.split("/");
        return comps[0];
    }

    private int getPage(int fid) {
        String key = "btt_page_" + fid;
        Config config = service.getConfig(key);
        if (config == null) {
            return 1;
        }

        return Integer.valueOf(config.getValue());
    }

    private void savePage(int fid, int page) {
        service.saveConfig("btt_page_" + fid, String.valueOf(page));
    }

}
