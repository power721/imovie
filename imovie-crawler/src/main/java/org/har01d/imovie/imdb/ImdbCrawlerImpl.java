package org.har01d.imovie.imdb;

import java.io.IOException;
import java.util.regex.Matcher;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.Imdb;
import org.har01d.imovie.domain.ImdbRepository;
import org.har01d.imovie.service.MovieService;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.UrlUtils;
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
public class ImdbCrawlerImpl implements ImdbCrawler {

    private static final Logger logger = LoggerFactory.getLogger(ImdbCrawlerImpl.class);

    @Value("${url.imdb}")
    private String baseUrl;

    @Autowired
    private ImdbRepository repository;

    @Autowired
    private MovieService service;

    @Override
    public void crawler() throws InterruptedException {
        Config full = service.getConfig("imdb_crawler");
        if (full != null) {
            logger.info("ignore ImdbCrawler");
            return;
        }

        int page = getPage();
        while (page <= 100) {
            String url = baseUrl + page;
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select(".lister-list .lister-item .lister-col-wrapper");
                if (elements.size() == 0) {
                    service.saveConfig("imdb_crawler", "full");
                    break;
                }

                for (Element element : elements) {
                    String href = element.select(".col-title .lister-item-header a").attr("href");
                    String imdb = getImdb(href);
                    String rating = element.select(".col-imdb-rating").text();
                    if ("-".equals(rating)) {
                        rating = null;
                    }
                    repository.save(new Imdb(imdb, rating));
                }
                page++;
                savePage(page);
            } catch (IOException e) {
                service.publishEvent(url, e.getMessage());
                logger.error("[xyw] Get HTML failed: " + url, e);
            }
        }
    }

    private String getImdb(String imdb) {
        Matcher matcher = UrlUtils.IMDB.matcher(imdb);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private int getPage() {
        String key = "imdb_page";
        Config config = service.getConfig(key);
        if (config == null) {
            return 1;
        }

        return Integer.valueOf(config.getValue());
    }

    private void savePage(int page) {
        service.saveConfig("imdb_page", String.valueOf(page));
    }

}
