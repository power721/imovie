package org.har01d.imovie.xyw;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
public class XywCrawlerImpl implements XywCrawler {

    private static final Logger logger = LoggerFactory.getLogger(XywCrawlerImpl.class);

    @Value("${url.xyw}")
    private String baseUrl;

    @Autowired
    private XywParser parser;

    @Autowired
    private MovieService service;

    @Override
    public void crawler() throws InterruptedException {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1, new MyThreadFactory("xyw"));
        executorService.scheduleWithFixedDelay(() -> work("movie"), 0, 5, TimeUnit.HOURS);
//        executorService.scheduleWithFixedDelay(() -> work("tv"), 0, 6, TimeUnit.HOURS);
    }

    private void work(String type) {
        int total = 0;
        int page = getPage(type);
        Config full = service.getConfig("xyw_crawler_" + type);
        while (true) {
            String url = baseUrl + "/" + type + "/?page=" + page;
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.row .movie-item .meta h1 a");
                if (elements.size() == 0) {
                    full = service.saveConfig("xyw_crawler_" + type, "full");
                    page = 1;
                    continue;
                }
                logger.info("[xyw] {}: {} movies", page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = element.attr("href");
                    if (pageUrl.startsWith("/")) {
                        pageUrl = baseUrl + pageUrl;
                    }
                    if (service.findSource(pageUrl) != null) {
                        continue;
                    }

                    Movie movie = new Movie();
                    movie.setTitle(element.attr("title"));
                    movie.setName(element.text());
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("[xyw] {}-{}-{} find movie {}", page, total, count, movie.getName());
                            service.save(new Source(pageUrl, movie.getSourceTime()));
                            count++;
                            total++;
                        } else {
                            service.save(new Source(pageUrl, false));
                        }
                    } catch (Exception e) {
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("[xyw] Parse page failed: " + pageUrl, e);
                    }
                }

                if (full != null && count == 0) {
                    break;
                }
                page++;
                savePage(type, page);
            } catch (IOException e) {
                service.publishEvent(url, e.getMessage());
                logger.error("[xyw] Get HTML failed: " + url, e);
            }
        }

        savePage(type, 1);
        logger.info("[xyw] ===== {}: get {} movies =====", type, total);
    }

    private int getPage(String type) {
        String key = "xyw_page_" + type;
        Config config = service.getConfig(key);
        if (config == null) {
            return 1;
        }

        return Integer.valueOf(config.getValue());
    }

    private void savePage(String type, int page) {
        service.saveConfig("xyw_page_" + type, String.valueOf(page));
    }

}
