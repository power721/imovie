package org.har01d.imovie.s80;

import java.util.Date;
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
public class S80CrawlerImpl extends AbstractCrawler implements S80Crawler {

    private static final Logger logger = LoggerFactory.getLogger(S80Crawler.class);

    @Value("${url.s80.site}")
    private String siteUrl;

    @Value("${url.s80.page}")
    private String baseUrl;

    @Autowired
    private S80Parser parser;

    private String[] types = {"movie/list/-----p", "ju/list/----0--p", "dm/list/----14--p", "zy/list/----4--p"};

    @Override
    public void crawler() throws InterruptedException {
        if (!checkTime()) {
            return;
        }

        work();
    }

    private void work() throws InterruptedException {
        int index = getConfig("index", 0);
        int page = getPage();
        Config crawler = getCrawlerConfig();
        while (index < types.length) {
            while (true) {
                handleError();
                String type = types[index];
                String url = String.format(baseUrl, type, page);

                try {
                    String html = HttpUtils.getHtml(url);
                    Document doc = Jsoup.parse(html);
                    error = 0;
                    Elements elements = doc.select("ul.me1 li h3 a");
                    if (elements.size() == 0) {
                        break;
                    }
                    logger.info("[80s]{}/{} {}: {} movies", index + 1, types.length, page, elements.size());

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
                                logger.info("[80s]{}/{} {}-{}-{} find movie {} {}", index + 1, types.length, page,
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
                            logger.error("[80s] Parse page failed: {}", pageUrl, e);
                        }
                    }

                    String text = doc.select(".pager a").last().text();
                    if (!(text.equals("尾页") || text.equals("下一页"))) {
                        logger.info("[80s] last page");
                        break;
                    }

                    if (crawler != null && count == 0) {
                        break;
                    }
                    sleep();
                } catch (Exception e) {
                    error++;
                    service.publishEvent(url, e.getMessage());
                    logger.error("[80s] Get HTML failed: {}", url, e);
                }
                savePage(++page);
            }
            saveConfig("index", ++index);
            page = 1;
            savePage(page);
        }

        saveCrawlerConfig();
        saveConfig("index", 0);
        savePage(1);
        logger.info("[80s] ===== get {} movies =====", total);
    }

    private Movie initMovie(Element element, Source source) {
        Movie movie = new Movie();
        if (source != null && source.getMovieId() != null) {
            movie.setId(source.getMovieId());
        }
        movie.setName(element.text().replace("未删减版", ""));
        return movie;
    }

}
