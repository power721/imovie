package org.har01d.imovie.rs05;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.http.impl.client.BasicCookieStore;
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
public class Rs05CrawlerImpl extends AbstractCrawler implements Rs05Crawler {

    private static final Logger logger = LoggerFactory.getLogger(Rs05Crawler.class);

    @Value("${url.rs05}")
    private String baseUrl;

    @Autowired
    private Rs05Parser parser;

    @Autowired
    private BasicCookieStore cookieStore;

    @Override
    public void crawler() throws InterruptedException {
        if (!checkTime()) {
            return;
        }

        int page = getPage();
        Config crawler = getCrawlerConfig();
        while (true) {
            handleError();
            String url = baseUrl + page;
            try {
                String html = HttpUtils.getHtml(url, "UTF-8", cookieStore);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("#movielist li");
                logger.info("{}: get {} movies", page, elements.size());
                if (elements.isEmpty()) {
                    if (crawler != null) {
                        break;
                    }
                    crawler = saveCrawlerConfig();
                    page = 1;
                    continue;
                }

                int count = 0;
                for (Element element : elements) {
                    Element header = element.select(".intro h2 a").first();
                    String title = header.attr("title");
                    String pageUrl = header.attr("href");
                    if (service.findSource(pageUrl) != null) {
                        continue;
                    }

                    String dbUrl = element.select(".intro .dou a").attr("href");

                    if (dbUrl.isEmpty()) {
                        service.publishEvent(pageUrl, "cannot get DouBan url");
                        logger.warn("cannot get DouBan url for {}", pageUrl);
                        continue;
                    }

                    Movie movie = new Movie();
                    movie.setName(title);
                    movie.setDbUrl(dbUrl);
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            service.save(new Source(pageUrl, getSourceTime(element)));
                            count++;
                            total++;
                        } else {
                            service.save(new Source(pageUrl, getSourceTime(element), false));
                            logger.warn("Cannot find movie {} from {}", title, pageUrl);
                            service.publishEvent(pageUrl, "Cannot find movie: " + title);
                        }
                        error = 0;
                    } catch (Exception e) {
                        error++;
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("Parse page failed: " + title, e);
                    }
                }

                if (crawler != null && count == 0) {
                    break;
                }
            } catch (Exception e) {
                error++;
                service.publishEvent(url, e.getMessage());
                logger.error("Get HTML failed: " + url, e);
            }
            page++;
            savePage(page);
        }

        saveCrawlerConfig();
        savePage(1);
        logger.info("[RS05]===== get {} movies =====", total);
    }

    private Date getSourceTime(Element element) {
        String text = element.select(".intro .tags").first().ownText();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return df.parse(text);
        } catch (ParseException e) {
            logger.warn("get time failed.", e);
        }
        return new Date();
    }

}
