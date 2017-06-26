package org.har01d.imovie.fix;

import java.io.IOException;
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
public class FixCrawlerImpl implements FixCrawler {

    private static final Logger logger = LoggerFactory.getLogger(FixCrawler.class);

    @Value("${url.fix}")
    private String baseUrl;

    @Autowired
    private FixParser parser;

    @Autowired
    private MovieService service;
    
    @Override
    public void crawler() throws InterruptedException {
        int total = 0;
        int page = getPage();
        Config full = service.getConfig("fix_crawler");
        while (true) {
            String url = baseUrl + page;
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div#portfolio-gallery .pg-items .pg-item a");
                if (elements.size() == 0) {
                    full = service.saveConfig("fix_crawler", "full");
                    page = 1;
                    continue;
                }
                logger.info("[fix] {}: {} movies", page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = element.attr("href");
                    Source source = service.findSource(pageUrl);
                    if (source != null) {
                        long time = System.currentTimeMillis();
                        if ((time - source.getUpdatedTime().getTime()) < 12 * 3600 * 1000L) {
                            logger.info("skip {}", pageUrl);
                            continue;
                        }
                    }

                    Movie movie = new Movie();
                    movie.setName(element.attr("title"));
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("[fix] {}-{}-{} find movie {}", page, total, count, movie.getName());
                            if (source == null) {
                                source = new Source(pageUrl, movie.getSourceTime());
                            }
                            count++;
                            total++;
                        } else {
                            if (source == null) {
                                source = new Source(pageUrl, false);
                            }
                        }
                        service.save(source);
                    } catch (Exception e) {
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("[fix] Parse page failed: " + pageUrl, e);
                    }
                }

                if (full != null && count == 0) {
                    break;
                }
                page++;
                savePage(page);
            } catch (IOException e) {
                service.publishEvent(url, e.getMessage());
                logger.error("[fix] Get HTML failed: " + url, e);
            }
        }

        savePage(1);
        logger.info("[fix] ===== get {} movies =====", total);
    }

    private int getPage() {
        String key = "fix_page";
        Config config = service.getConfig(key);
        if (config == null) {
            return 1;
        }

        return Integer.valueOf(config.getValue());
    }

    private void savePage(int page) {
        service.saveConfig("fix_page", String.valueOf(page));
    }

}
