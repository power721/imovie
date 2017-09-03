package org.har01d.imovie.zmz;

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
public class ZmzCrawlerImpl extends AbstractCrawler implements ZmzCrawler {

    private static final Logger logger = LoggerFactory.getLogger(ZmzCrawler.class);

    @Value("${url.zmz.site}")
    private String siteUrl;

    @Value("${url.zmz.page}")
    private String baseUrl;

    @Autowired
    private ZmzParser parser;

    @Override
    public void crawler() throws InterruptedException {
        int page = getPage();
        Config crawler = getCrawlerConfig();
        while (true) {
            handleError();
            String url = baseUrl + page;
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.resource-showlist ul li .fl-info");
                if (elements.size() == 0) {
                    if (crawler != null) {
                        break;
                    }
                    crawler = saveCrawlerConfig();
                    page = 1;
                    continue;
                }
                logger.info("[zmz] {}: {} movies", page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = siteUrl + element.select("h3 a").attr("href");
                    Source source = service.findSource(pageUrl);
                    if (source != null) {
                        if (source.isCompleted()) {
                            continue;
                        }

                        long time = System.currentTimeMillis();
                        if ((time - source.getUpdatedTime().getTime()) < TimeUnit.HOURS.toMillis(12)) {
                            logger.info("skip {}", pageUrl);
                            continue;
                        }
                    }

                    boolean completed = false;
                    if (element.select("h3").text().contains("[本剧完结]")) {
                        completed = true;
                    }

                    Movie movie = new Movie();
                    if (source != null && source.getMovieId() != null) {
                        movie.setId(source.getMovieId());
                    }

                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger
                                .info("[zmz] {}-{}-{} find movie {} {}", page, total, count, movie.getName(), pageUrl);
                            if (source == null) {
                                source = new Source(pageUrl, getSourceTime(element));
                            }
                            source.setMovieId(movie.getId());
                            if (crawler == null || movie.getNewResources() > 0) {
                                count++;
                            }
                            total++;
                        } else {
                            if (source == null) {
                                source = new Source(pageUrl, completed);
                            }
                        }
                        source.setCompleted(completed);
                        source.setUpdatedTime(new Date());
                        service.save(source);
                        error = 0;
                    } catch (Exception e) {
                        error++;
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("[zmz] Parse page failed: " + pageUrl, e);
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
                logger.error("[zmz] Get HTML failed: " + url, e);
            }
        }

        saveCrawlerConfig();
        savePage(1);
        logger.info("[zmz] ===== get {} movies =====", total);
    }

    private Date getSourceTime(Element element) {
        String text = element.select("span.dateline").attr("time");
        try {
            int time = Integer.parseInt(text);
            return new Date(time * 1000L);
        } catch (Exception e) {
            logger.warn("get time failed.", e);
        }
        return new Date();
    }

}
