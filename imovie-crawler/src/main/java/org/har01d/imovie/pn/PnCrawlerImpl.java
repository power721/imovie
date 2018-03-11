package org.har01d.imovie.pn;

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
public class PnCrawlerImpl extends AbstractCrawler implements PnCrawler {

    private static final Logger logger = LoggerFactory.getLogger(PnCrawler.class);

    private static final String[] types = {"movie", "tv", "comic", "doc"};

    @Value("${url.pn}")
    private String baseUrl;

    @Autowired
    private PnParser parser;

    @Override
    public void crawler() throws InterruptedException {
        Config crawler = getCrawlerConfig();
        if (!checkTime(crawler)) {
            return;
        }

        for (int i = getConfig("index", 0); i < types.length; ++i) {
            saveConfig("index", i);
            int page = getPage();
            while (page <= 100) {
                handleError();
                String url = baseUrl + types[i] + "/pn" + page + ".html";
                try {
                    String html = HttpUtils.getHtml(url);
                    Document doc = Jsoup.parse(html);
                    Elements elements = doc.select("div.movies div.movieFlag .title .titleInnner a");
                    if (elements.size() == 0) {
                        break;
                    }
                    logger.info("[pn] {}: {} movies", page, elements.size());

                    int count = 0;
                    for (Element element : elements) {
                        String pageUrl = element.attr("href");
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
                                logger.info("[pn] {}-{}-{} find movie {}", page, total, count, movie.getName());
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
                            logger.error("[pn] Parse page failed: " + pageUrl, e);
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
                    logger.error("[pn] Get HTML failed: " + url, e);
                }
            }
            savePage(1);
        }

        saveCrawlerConfig();
        saveConfig("index", 0);
        savePage(1);
        logger.info("[pn] ===== get {} movies =====", total);
    }

}
