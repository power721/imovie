package org.har01d.imovie.bttt;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
public class BtttCrawlerImpl implements BtttCrawler {

    private static final Logger logger = LoggerFactory.getLogger(BtttCrawler.class);

    @Value("${url.bttt.site}")
    private String siteUrl;

    @Value("${url.bttt.page}")
    private String baseUrl;

    @Autowired
    private BttttParser parser;

    @Autowired
    private MovieService service;

    @Override
    public void crawler() throws InterruptedException {
        int total = 0;
        int error = 0;
        int page = getPage();
        Config full = service.getConfig("bttt_crawler");
        while (true) {
            String url = String.format(baseUrl, page);
            try {
                if (error >= 5) {
                    logger.warn("sleep {} seconds", error * 30L);
                    TimeUnit.SECONDS.sleep(error * 30L);
                }

                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.ml .item .title");
                if (elements.size() == 0) {
                    full = service.saveConfig("bttt_crawler", "full");
                    page = 0;
                    continue;
                }
                logger.info("[bttiantang] {}: {} movies", page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = siteUrl + element.select(".tt a").attr("href");
                    if (service.findSource(pageUrl) != null) {
                        continue;
                    }

                    Movie movie = new Movie();
                    movie.setTitle(element.select(".tt a").text());
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("[bttiantang] {}-{}-{} find movie {}", page, total, count, movie.getName());
                            movie.setSourceTime(getSourceTime(element.select(".tt span").text()));
                            service.save(new Source(pageUrl, movie.getSourceTime()));
                            count++;
                            total++;
                        } else {
                            service.save(new Source(pageUrl, getSourceTime(element.select(".tt span").text()), false));
                        }
                        error = 0;
                    } catch (Exception e) {
                        error++;
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("[bttiantang] Parse page failed: " + pageUrl, e);
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
                logger.error("[bttiantang] Get HTML failed: " + url, e);
            }
        }

        savePage(0);
        logger.info("[bttiantang] ===== get {} movies =====", total);
    }

    private Date getSourceTime(String text) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return df.parse(text);
        } catch (ParseException e) {
            logger.warn("[bttiantang] get time failed.", e);
        }
        return new Date();
    }

    private int getPage() {
        String key = "bttt_page";
        Config config = service.getConfig(key);
        if (config == null) {
            return 0;
        }

        return Integer.valueOf(config.getValue());
    }

    private void savePage(int page) {
        service.saveConfig("bttt_page", String.valueOf(page));
    }

}
