package org.har01d.imovie.mp4;

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
public class Mp4CrawlerImpl extends AbstractCrawler implements Mp4Crawler {

    private static final Logger logger = LoggerFactory.getLogger(Mp4Crawler.class);

    @Value("${url.mp4.site}")
    private String siteUrl;

    @Value("${url.mp4.page}")
    private String baseUrl;

    @Autowired
    private Mp4Parser parser;

    @Override
    public void crawler() throws InterruptedException {
        int total = 0;
        int error = 0;
        int page = getPage();
        Config crawler = getCrawlerConfig();
        while (true) {
            String url = baseUrl + page;
            try {
                if (error >= 5) {
                    logger.warn("sleep {} seconds", error * 30L);
                    TimeUnit.SECONDS.sleep(error * 30L);
                }

                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.shannel ul li h2 a");
                if (elements.size() == 0) {
                    crawler = saveCrawlerConfig();
                    page = 1;
                    continue;
                }
                logger.info("[mp4] {}: {} movies", page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = siteUrl + element.attr("href");
                    Source source = service.findSource(pageUrl);
                    if (source != null) {
                        logger.info("skip {}", pageUrl);
                        continue;
                    }

                    Movie movie = new Movie();
                    movie.setName(element.text());
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("[mp4] {}-{}-{} find movie {}", page, total, count, movie.getName());
                            source = new Source(pageUrl, movie.getSourceTime());
                            count++;
                            total++;
                        } else {
                            source = new Source(pageUrl, false);
                        }
                        service.save(source);
                        error = 0;
                    } catch (Exception e) {
                        error++;
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("[mp4] Parse page failed: " + pageUrl, e);
                    }
                }

                if (!doc.select("div.page a").last().text().equals("尾页")) {
                    crawler = saveCrawlerConfig();
                    page = 1;
                    continue;
                }

                if (crawler != null && count == 0) {
                    break;
                }
                page++;
                savePage(page);
            } catch (Exception e) {
                error++;
                service.publishEvent(url, e.getMessage());
                logger.error("[mp4] Get HTML failed: " + url, e);
            }
        }

        saveCrawlerConfig();
        savePage(1);
        logger.info("[mp4] ===== get {} movies =====", total);
    }

}
