package org.har01d.imovie.btdy;

import java.util.concurrent.TimeUnit;
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
public class BtdyCrawlerImpl implements BtdyCrawler {

    private static final Logger logger = LoggerFactory.getLogger(BtdyCrawler.class);

    @Value("${url.btdy.site}")
    private String siteUrl;

    @Value("${url.btdy.page}")
    private String baseUrl;

    @Autowired
    private BtdyParser parser;

    @Autowired
    private MovieService service;

    @Override
    public void crawler() throws InterruptedException {
        int error = 0;
        int total = 0;
        int page = getPage();
        Config full = service.getConfig("btdy_crawler");
        while (true) {
            String url = String.format(baseUrl, page);
            try {
                if (error >= 5) {
                    logger.warn("sleep {} seconds", error * 30L);
                    TimeUnit.SECONDS.sleep(error * 30L);
                }

                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);

                int last = 9999;
                try {
                    last = Integer.valueOf(doc.select("div.pages").first().children().last().text());
                } catch (NumberFormatException e) {
                    // ignore
                }
                if (page > last) {
                    full = service.saveConfig("btdy_crawler", "full");
                    page = 1;
                    continue;
                }

                Elements elements = doc.select("div.list_su dl .title a");
                logger.info("[btbtdy] {}: {} movies", page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = siteUrl + element.attr("href");
                    if (service.findSource(pageUrl) != null) {
                        continue;
                    }

                    Movie movie = new Movie();
                    movie.setName(element.attr("title").replaceAll("全.季", ""));
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("[btbtdy] {}-{}-{} find movie {}", page, total, count, movie.getName());
                            service.save(new Source(pageUrl, movie.getSourceTime()));
                            count++;
                            total++;
                        } else {
                            service.save(new Source(pageUrl, false));
                        }
                        error = 0;
                    } catch (Exception e) {
                        error++;
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("[btbtdy] Parse page failed: " + pageUrl, e);
                    }
                }

                if (full != null && count == 0) {
                    break;
                }
                page++;
                savePage(page);
            } catch (Exception e) {
                error++;
                service.publishEvent(url, e.getMessage());
                logger.error("[btbtdy] Get HTML failed: " + url, e);
            }
        }

        savePage(1);
        logger.info("[btbtdy] ===== get {} movies =====", total);
    }

    private int getPage() {
        String key = "btdy_page";
        Config config = service.getConfig(key);
        if (config == null) {
            return 1;
        }

        return Integer.valueOf(config.getValue());
    }

    private void savePage(int page) {
        service.saveConfig("btdy_page", String.valueOf(page));
    }

}
