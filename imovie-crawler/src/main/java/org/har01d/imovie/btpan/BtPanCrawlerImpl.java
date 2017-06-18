package org.har01d.imovie.btpan;

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
public class BtPanCrawlerImpl implements BtPanCrawler {

    private static final Logger logger = LoggerFactory.getLogger(BtPanCrawler.class);

    @Value("${url.btpan}")
    private String baseUrl;

    @Autowired
    private BtPanParser parser;

    @Autowired
    private MovieService service;

    @Override
    public void crawler() throws InterruptedException {
        int total = 0;
        int page = getPage();
        Config full = service.getConfig("btpan_crawler");
        while (true) {
            String url = baseUrl + "/?page=" + page;
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.content .item .title a");
                if (elements.size() == 0) {
                    full = service.saveConfig("btpan_crawler", "full");
                    page = 1;
                    continue;
                }
                logger.info("[btpan] {}: {} movies", page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = baseUrl + element.attr("href");
                    if (service.findSource(pageUrl) != null) {
                        continue;
                    }

                    Movie movie = new Movie();
                    movie.setTitle(element.attr("title"));
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("[btpan] {}-{}-{} find movie {}", page, total, count, movie.getName());
                            service.save(new Source(pageUrl, movie.getSourceTime()));
                            count++;
                            total++;
                        }
                    } catch (Exception e) {
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("[btpan] Parse page failed: " + pageUrl, e);
                    }
                }

                if (full != null && count == 0) {
                    break;
                }
                page++;
                savePage(page);
            } catch (IOException e) {
                service.publishEvent(url, e.getMessage());
                logger.error("[btpan] Get HTML failed: " + url, e);
            }
        }

        savePage(1);
        logger.info("[btpan] ===== get {} movies =====", total);
    }

    private int getPage() {
        String key = "btpan_page";
        Config config = service.getConfig(key);
        if (config == null) {
            return 1;
        }

        return Integer.valueOf(config.getValue());
    }

    private void savePage(int page) {
        service.saveConfig("btpan_page", String.valueOf(page));
    }

}
