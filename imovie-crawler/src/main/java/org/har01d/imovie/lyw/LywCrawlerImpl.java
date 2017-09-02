package org.har01d.imovie.lyw;

import java.util.Date;
import org.apache.http.client.HttpResponseException;
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
public class LywCrawlerImpl extends AbstractCrawler implements LywCrawler {

    private static final Logger logger = LoggerFactory.getLogger(LywCrawler.class);

    @Value("${url.lyw.site}")
    private String siteUrl;

    @Value("${url.lyw.page}")
    private String baseUrl;

    @Autowired
    private LywParser parser;

    private int[] ids = {99, 86, 84, 98, 97, 96, 95, 94, 93, 92, 91, 89, 81, 80, 83, 82, 90, 79, 13, 12, 11, 10, 9, 8};

    @Override
    public void crawler() throws InterruptedException {
        work();
    }

    private void work() throws InterruptedException {
        int index = getConfig("index", 0);
        int page = getPage();
        Config crawler = getCrawlerConfig();
        while (index < ids.length) {
            while (true) {
                handleError();
                int id = ids[index];
                String url = String.format(baseUrl, id, page);

                try {
                    String html = HttpUtils.getHtml(url);
                    Document doc = Jsoup.parse(html);
                    error = 0;
                    Elements elements = doc.select("div.news_con ul li div.col-lg-9 h2 a");
                    if (elements.size() == 0) {
                        break;
                    }
                    logger.info("[lyw-{}]{}/{} {}: {} movies", id, index + 1, ids.length, page, elements.size());

                    int count = 0;
                    for (Element element : elements) {
                        String pageUrl = siteUrl + element.attr("href");
                        if (!addOrUpdate(pageUrl)) {
                            continue;
                        }
                        Source source = service.findSource(pageUrl);

                        Movie movie = initMovie(element, source);
                        try {
                            movie = parser.parse(pageUrl, movie);
                            if (movie != null) {
                                logger.info("[lyw-{}]{}/{} {}-{}-{} find movie {} {}", id, index + 1, ids.length, page,
                                    total, count,
                                    movie.getName(),
                                    pageUrl);
                                if (source == null) {
                                    source = new Source(pageUrl, movie.getSourceTime(), movie.isCompleted());
                                }
                                source.setMovieId(movie.getId());
                                if (crawler == null || movie.getNewResources() > 0) {
                                    count++;
                                }
                                total++;
                            } else {
                                if (source == null) {
                                    source = new Source(pageUrl, false);
                                }
                            }
                            source.setUpdatedTime(new Date());
                            service.save(source);
                            error = 0;
                        } catch (Exception e) {
                            error++;
                            service.publishEvent(pageUrl, e.getMessage());
                            logger.error("[lyw-{}] Parse page failed: {}", id, pageUrl, e);
                        }
                    }

                    if (crawler != null && count == 0) {
                        break;
                    }
                    sleep();
                } catch (Exception e) {
                    if (e instanceof HttpResponseException && ((HttpResponseException) e).getStatusCode() == 404) {
                        break;
                    }
                    error++;
                    service.publishEvent(url, e.getMessage());
                    logger.error("[lyw-{}] Get HTML failed: {}", id, url, e);
                }
                savePage(++page);
            }
            saveConfig("index", ++index);
            page = 1;
            savePage(page);
        }

        saveCrawlerConfig();
        saveConfig("index", 1);
        savePage(1);
        logger.info("[lyw] ===== get {} movies =====", total);
    }

    private Movie initMovie(Element element, Source source) {
        Movie movie = new Movie();
        if (source != null && source.getMovieId() != null) {
            movie.setId(source.getMovieId());
        }
        movie.setName(element.attr("title"));
        return movie;
    }

}
