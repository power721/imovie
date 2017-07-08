package org.har01d.imovie.btpan;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.har01d.imovie.AbstractCrawler;
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
public class BtPanCrawlerImpl extends AbstractCrawler implements BtPanCrawler {

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
        int error = 0;
        int page = getPage();
        Config crawler = getCrawlerConfig();
        while (true) {
            String url = baseUrl + "/?page=" + page;
            try {
                if (error >= 5) {
                    logger.warn("[btpan] sleep {} seconds", error * 30L);
                    TimeUnit.SECONDS.sleep(error * 30L);
                }
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.content .item .title a");
                if (elements.size() == 0) {
                    crawler = saveCrawlerConfig();
                    page = 1;
                    error = 0;
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
                        } else {
                            service.save(new Source(pageUrl, false));
                        }
                        error = 0;
                    } catch (Exception e) {
                        error++;
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("[btpan] Parse page failed: " + pageUrl, e);
                    }
                }

                if (crawler != null && count == 0) {
                    break;
                }
                page++;
                savePage(page);
            } catch (IOException e) {
                error++;
                service.publishEvent(url, e.getMessage());
                logger.error("[btpan] Get HTML failed: " + url, e);
            }
        }

        saveCrawlerConfig();
        savePage(1);
        logger.info("[btpan] ===== get {} movies =====", total);
    }

}
