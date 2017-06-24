package org.har01d.imovie.btapple;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
public class BtaCrawlerImpl implements BtaCrawler {

    private static final Logger logger = LoggerFactory.getLogger(BtaCrawler.class);

    @Value("${url.btapple.site}")
    private String siteUrl;

    @Value("${url.btapple.page}")
    private String baseUrl;

    @Autowired
    private BtaParser parser;

    @Autowired
    private MovieService service;

    @Override
    public void crawler() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2, new MyThreadFactory("BtApple"));
        executorService.submit(() -> work(1, "movie"));
        executorService.submit(() -> work(3, "tv"));
        executorService.shutdown();
    }

    private void work(int id, String type) {
        int total = 0;
        int page = getPage(type);
        Config full = service.getConfig("bta_crawler_" + type);
        while (true) {
            String url = String.format(baseUrl, type, id, page);
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.row .info a");
                if (elements.size() == 0) {
                    full = service.saveConfig("bta_crawler_" + type, "full");
                    page = 0;
                    continue;
                }
                logger.info("[BtApple] {}: {} movies", page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = siteUrl + element.attr("href");
                    if (service.findSource(pageUrl) != null) {
                        continue;
                    }

                    Movie movie = new Movie();
                    movie.setTitle(element.attr("title"));
                    movie.setName(element.text());
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("[BtApple] {}-{}-{} find movie {}", page, total, count, movie.getName());
                            service.save(new Source(pageUrl, movie.getSourceTime()));
                            count++;
                            total++;
                        } else {
                            service.save(new Source(pageUrl, false));
                        }
                    } catch (Exception e) {
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("[BtApple] Parse page failed: " + pageUrl, e);
                    }
                }

                if (full != null && count == 0) {
                    break;
                }
                page++;
                savePage(type, page);
            } catch (IOException e) {
                service.publishEvent(url, e.getMessage());
                logger.error("[BtApple] Get HTML failed: " + url, e);
            }
        }

        savePage(type, 0);
        logger.info("[BtApple] ===== {}: get {} movies =====", type, total);
    }

    private int getPage(String type) {
        String key = "bta_page_" + type;
        Config config = service.getConfig(key);
        if (config == null) {
            return 0;
        }

        return Integer.valueOf(config.getValue());
    }

    private void savePage(String type, int page) {
        service.saveConfig("bta_page_" + type, String.valueOf(page));
    }

}
