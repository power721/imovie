package org.har01d.imovie.mjtt;

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
public class MjttCrawlerImpl extends AbstractCrawler implements MjttCrawler {

    private static final Logger logger = LoggerFactory.getLogger(MjttCrawler.class);

    @Value("${url.mjtt.site}")
    private String siteUrl;

    @Value("${url.mjtt.page}")
    private String baseUrl;

    @Autowired
    private MjttParser parser;

    @Override
    public void crawler() throws InterruptedException {
        work();
    }

    private void work() throws InterruptedException {
        Config crawler = getCrawlerConfig();
        if (!checkTime(crawler)) {
            return;
        }

        int index = getConfig("index", 1);
        int page = getPage();
        while (index <= 6) {
            while (true) {
                handleError();
                String url = String.format(baseUrl, index, page);
                if (page == 1) {
                    url = url.replace("_1", "");
                }

                try {
                    String html = HttpUtils.getHtml(url, "GBK");
                    Document doc = Jsoup.parse(html);
                    error = 0;
                    Elements elements = doc.select("div.cn_box2 ul");
                    if (elements.size() == 0) {
                        break;
                    }
                    logger.info("[mjtt-{}] {}: {} movies", index, page, elements.size());

                    int count = 0;
                    for (Element element : elements) {
                        String pageUrl = siteUrl + element.select("li a").attr("href");
                        if (!addOrUpdate(pageUrl)) {
                            continue;
                        }
                        Source source = service.findSource(pageUrl);

                        Movie movie = initMovie(element.select("li a").first(), source);
                        try {
                            movie = parser.parse(pageUrl, movie);
                            if (movie != null) {
                                logger.info("[mjtt-{}] {}-{}-{} find movie {} {}", index, page, total, count,
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
                            logger.error("[mjtt-{}] Parse page failed: {}", index, pageUrl, e);
                        }
                    }

                    if (isLastPage(doc)) {
                        break;
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
                    logger.error("[mjtt-{}] Get HTML failed: {}", index, url, e);
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
        logger.info("[mjtt] ===== get {} movies =====", total);
    }

    private Movie initMovie(Element element, Source source) {
        Movie movie = new Movie();
        if (source != null && source.getMovieId() != null) {
            movie.setId(source.getMovieId());
        }
        movie.setName(element.text());
        return movie;
    }

    private boolean isLastPage(Document doc) {
        return !doc.select("div.page a").last().text().equals("尾页");
    }

}
