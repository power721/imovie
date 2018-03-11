package org.har01d.imovie.bt0;

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
public class Bt0CrawlerImpl extends AbstractCrawler implements Bt0Crawler {

    private static final Logger logger = LoggerFactory.getLogger(Bt0Crawler.class);

    private static final String[] types = {"film", "tv"};
    private static final Integer[] ids = {1, 3};

    @Value("${url.bt0}")
    private String baseUrl;

    @Autowired
    private Bt0Parser parser;

    @Override
    public void crawler() throws InterruptedException {
        Config crawler = getCrawlerConfig();
        if (!checkTime(crawler)) {
            return;
        }

        for (int i = getConfig("index", 0); i < types.length; ++i) {
            saveConfig("index", i);
            int page = getPage(0);
            while (true) {
                handleError();
                String url = baseUrl + types[i] + "-download/" + ids[i] + "-0-0-0-0-" + page + ".html";
                try {
                    String html = HttpUtils.getHtml(url);
                    Document doc = Jsoup.parse(html);
                    Elements elements = doc.select("div.masonry div.masonry__item a");
                    if (elements.size() == 0) {
                        break;
                    }
                    logger.info("[bt0-{}] {}: {} movies", i, page, elements.size());

                    int count = 0;
                    for (Element element : elements) {
                        String pageUrl = baseUrl + element.attr("href");
                        Source source = service.findSource(pageUrl);
                        if (source != null) {
                            logger.info("skip {}", pageUrl);
                            continue;
                        }

                        Movie movie = new Movie();
                        movie.setName(element.select("h2").text());
                        try {
                            movie = parser.parse(pageUrl, movie);
                            if (movie != null) {
                                logger.info("[bt0-{}] {}-{}-{} find movie {}", i, page, total, count, movie.getName());
                                source = new Source(pageUrl, movie.getSourceTime());
                                source.setMovieId(movie.getId());
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
                            logger.error("[bt0] Parse page failed: " + pageUrl, e);
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
                    logger.error("[bt0] Get HTML failed: " + url, e);
                }
            }
            savePage(0);
        }

        saveCrawlerConfig();
        saveConfig("index", 0);
        savePage(0);
        logger.info("[bt0] ===== get {} movies =====", total);
    }

}
