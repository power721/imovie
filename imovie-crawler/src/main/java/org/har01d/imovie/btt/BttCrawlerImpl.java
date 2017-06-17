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
        .compile("^\\[([^]]+)] \\[[^]]+] \\[([^]]+)] (?:\\[连载] )?(?:\\[打包] )?(?:\\[全集] )?(?:\\[合集] )?\\[[^]]+](.*)$");

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
        ExecutorService executorService = Executors.newFixedThreadPool(3, new MyThreadFactory("BttCrawler"));
        executorService.submit(() -> work(951));
//        executorService.submit(() -> work(981));
        executorService.submit(() -> work(1183));
        executorService.submit(() -> work(950));
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
                Elements elements = doc.select("td.subject");
                int last = 99999;
                try {
                    last = Integer.valueOf(doc.select("div.page a.checked").text());
                } catch (NumberFormatException e) {
                    // ignore
                }
                if (page > last || elements.size() == 0) {
                    full = service.saveConfig("btt_crawler_" + fid, "full");
                    page = 1;
                    continue;
                }

                int count = 0;
                for (Element element : elements) {
                    String text = element.text();
                    Movie movie = new Movie();
                    String pageUrl = siteUrl + element.select("a.subject_link").attr("href");
                    if (service.findSource(pageUrl) != null) {
                        continue;
                    }
                    movie.setTitle(element.select("a.subject_link").text());
                    movie.setName(getName(movie.getTitle()));
                    Matcher matcher = SUBJECT_PATTERN.matcher(text);
                    if (matcher.find()) {
                        String str = matcher.group(3);
                        String name = str;
                        if (str.contains("BT") || str.contains("下载") || str.contains("网盘")) {
                            name = matcher.group(4);
                        }
                        name = getName(name);
                        logger.info(fid + "-" + page + "-" + total + "-" + count + " " + name + ": " + pageUrl);

                        String y = matcher.group(1);
                        movie.setTitle(text);
                        movie.setName(name);
                        if (y.matches("\\d{4}")) {
                            movie.setYear(Integer.valueOf(y));
                        }
                        movie.setCategories(service.getCategories(Collections.singleton(matcher.group(2))));
                    } else {
                        matcher = SUBJECT_PATTERN2.matcher(text);
                        if (matcher.find()) {
                            String name = getName(matcher.group(3));
                            logger.info(fid + "-" + page + "-" + total + "-" + count + " " + name + ": " + pageUrl);

                            String y = matcher.group(1);
                            movie.setName(name);
                            movie.setTitle(text);
                            if (y.matches("\\d{4}")) {
                                movie.setYear(Integer.valueOf(y));
                            }
                            movie.setCategories(service.getCategories(Collections.singleton(matcher.group(2))));
                        }
                    }

                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            service.save(new Source(pageUrl, movie.getSourceTime()));
                            count++;
                            total++;
                        } else {
//                            service.save(new Source(pageUrl));
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
        logger.info("===== {}: get {} movies =====", fid, total);
    }

    private String getName(String title) {
        if (title.startsWith("[")) {
            int index = title.indexOf(']');
            String temp = title.substring(1, index);
            if (temp.contains("BT") || temp.contains("电驴") || temp.contains("下载") || temp.contains("网盘")
                || temp.contains("三立") || temp.contains("民视")
                || temp.contains("TVB") || temp.contains("ATV") || temp.contains("HKTV") || temp.contains("Viu TV")) {
                int start = index + 1;
                index = title.indexOf(']', start);
                if (index < 0) {
                    index = title.length();
                }
                temp = title.substring(start, index);
            }
            title = temp;
        }

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
