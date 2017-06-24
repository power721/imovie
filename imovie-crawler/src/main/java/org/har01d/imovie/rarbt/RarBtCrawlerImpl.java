package org.har01d.imovie.rarbt;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Source;
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
public class RarBtCrawlerImpl implements RarBtCrawler {

    private static final Logger logger = LoggerFactory.getLogger(RarBtCrawler.class);

    @Value("${url.rarbt.site}")
    private String siteUrl;

    @Value("${url.rarbt.page}")
    private String baseUrl;

    @Autowired
    private RarBtParser parser;

    @Autowired
    private MovieService service;

    @Override
    public void crawler() throws InterruptedException {
        int total = 0;
        int page = getPage();
        Config full = service.getConfig("rarbt_crawler");
        while (true) {
            String url = String.format(baseUrl, page);
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.ml .item .title");
                if (elements.size() == 0) {
                    full = service.saveConfig("rarbt_crawler", "full");
                    page = 1;
                    continue;
                }
                logger.info("{}: {} movies", page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = siteUrl + element.select(".tt a").attr("href");
                    if (service.findSource(pageUrl) != null) {
                        continue;
                    }

                    String db = siteUrl + element.select(".rt a").attr("href");
                    String dbUrl = getDbUrl(db);
                    Movie movie = new Movie();
                    movie.setDbUrl(dbUrl);
                    movie.setTitle(element.select(".tt a").text());

                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("{}-{}-{} find movie {}", page, total, count, movie.getName());
                            movie.setSourceTime(getSourceTime(element.select(".tt span").text()));
                            service.save(new Source(pageUrl, movie.getSourceTime()));
                            count++;
                            total++;
                        } else {
                            service.save(new Source(pageUrl, getSourceTime(element.select(".tt span").text()), false));
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
                savePage(page);
            } catch (IOException e) {
                service.publishEvent(url, e.getMessage());
                logger.error("Get HTML failed: " + url, e);
            }
        }

        savePage(1);
        logger.info("===== get {} movies =====", total);
    }

    private String getDbUrl(String redirectUrl) throws IOException {
        String html = HttpUtils.getHtml(redirectUrl);
        return UrlUtils.getDbUrl(html);
    }

    private Date getSourceTime(String text) {
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        try {
            return df.parse(text);
        } catch (ParseException e) {
            logger.warn("get time failed.", e);
        }
        return new Date();
    }

    private int getPage() {
        String key = "rarbt_page";
        Config config = service.getConfig(key);
        if (config == null) {
            return 1;
        }

        return Integer.valueOf(config.getValue());
    }

    private void savePage(int page) {
        service.saveConfig("rarbt_page", String.valueOf(page));
    }

}
