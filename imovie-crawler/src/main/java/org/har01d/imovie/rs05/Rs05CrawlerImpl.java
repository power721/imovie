package org.har01d.imovie.rs05;

import java.net.SocketTimeoutException;
import org.apache.http.impl.client.BasicCookieStore;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Source;
import org.har01d.imovie.douban.DouBanParser;
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
public class Rs05CrawlerImpl implements Rs05Crawler {

    private static final Logger logger = LoggerFactory.getLogger(Rs05Crawler.class);

    @Value("${url.rs05}")
    private String baseUrl;

    @Autowired
    private Rs05Parser parser;

    @Autowired
    private DouBanParser douBanParser;

    @Autowired
    private MovieService service;

    @Autowired
    private BasicCookieStore cookieStore;

    @Override
    public void crawler() throws InterruptedException {
        int total = 0;
        int page = getPage();
        while (true) {
            String url = baseUrl + page;
            try {
                String html = HttpUtils.getHtml(url, "UTF-8", cookieStore);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("#movielist li");
                logger.info("{}: get {} movies", page, elements.size());
                if (elements.isEmpty()) {
                    break;
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
                        logger.warn("cannot get douban url for {}", pageUrl);
                        continue;
                    }

                    Movie movie = service.findByDbUrl(dbUrl);
                    if (movie == null) {
                        try {
                            movie = douBanParser.parse(dbUrl);
                            service.save(movie);
                        } catch (Exception e) {
                            service.publishEvent(dbUrl, e.getMessage());
                            logger.error("Parse page failed: " + title, e);
                        }
                    }

                    if (movie != null) {
                        try {
                            parser.parse(pageUrl, movie);
                            service.save(new Source(pageUrl));
                            count++;
                            total++;
                        } catch (Exception e) {
                            service.publishEvent(pageUrl, e.getMessage());
                            logger.error("Parse page failed: " + title, e);
                        }
                    } else {
                        logger.warn("Cannot find movie for " + pageUrl);
                        service.publishEvent(pageUrl, "Cannot find movie");
                    }
                }

                if (count == 0) {
                    break;
                }
            } catch (SocketTimeoutException e) {
                service.publishEvent(url, e.getMessage());
                logger.error("Get HTML failed: " + url, e);
                break;
            } catch (Exception e) {
                service.publishEvent(url, e.getMessage());
                logger.error("Get HTML failed: " + url, e);
            }
            page++;
            savePage(page);
        }

        savePage(1);
        logger.info("===== get {} movies =====", total);
    }

    private int getPage() {
        String key = "rs05_page";
        Config config = service.getConfig(key);
        if (config == null) {
            return 1;
        }

        return Integer.valueOf(config.getValue());
    }

    private void savePage(int page) {
        service.saveConfig("rs05_page", String.valueOf(page));
    }

}
