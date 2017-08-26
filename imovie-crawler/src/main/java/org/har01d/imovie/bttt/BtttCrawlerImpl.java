package org.har01d.imovie.bttt;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.har01d.imovie.AbstractCrawler;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Source;
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
public class BtttCrawlerImpl extends AbstractCrawler implements BtttCrawler {

    private static final Logger logger = LoggerFactory.getLogger(BtttCrawler.class);

    @Value("${url.bttt.site}")
    private String siteUrl;

    @Value("${url.bttt.page}")
    private String baseUrl;

    @Autowired
    private BttttParser parser;

    @Override
    public void crawler() throws InterruptedException {
        int total = 0;
        int error = 0;
        int page = getPage(0);
        Config crawler = getCrawlerConfig();
        while (true) {
            String url = String.format(baseUrl, page);
            try {
                if (error >= 5) {
                    if (error >= 10) {
                        return;
                    }
                    logger.warn("sleep {} seconds", error * 30L);
                    TimeUnit.SECONDS.sleep(error * 30L);
                }

                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.ml .item .title");
                if (elements.size() == 0) {
                    crawler = saveCrawlerConfig();
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

                if (crawler != null && count == 0) {
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

        saveCrawlerConfig();
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

}
