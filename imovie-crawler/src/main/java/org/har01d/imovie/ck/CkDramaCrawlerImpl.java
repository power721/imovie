package org.har01d.imovie.ck;

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
public class CkDramaCrawlerImpl extends AbstractCrawler implements CkDramaCrawler {

    private static final Logger logger = LoggerFactory.getLogger(CkCrawler.class);

    @Value("${url.ck.drama}")
    private String baseUrl;

    @Autowired
    private CkParser parser;

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
                Elements elements = doc.select("div.boutlist ul li p.no a");
                if (elements.size() == 0) {
                    crawler = saveCrawlerConfig();
                    page = 1;
                    continue;
                }
                logger.info("[ck-tv] {}: {} movies", page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = element.attr("href");
                    Source source = service.findSource(pageUrl);
                    if (source != null) {
                        if (source.isCompleted()) {
                            logger.info("skip {}", pageUrl);
                            continue;
                        }

                        long time = System.currentTimeMillis();
                        if ((time - source.getUpdatedTime().getTime()) < TimeUnit.HOURS.toMillis(12)) {
                            logger.info("skip {}", pageUrl);
                            continue;
                        }
                    }

                    boolean completed = false;
                    if (element.select("div.boutlist ul li p.slz").text().contains("全集")) {
                        completed = true;
                    }

                    Movie movie = new Movie();
                    movie.setName(getName(element.text()));
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("[ck-tv] {}-{}-{} find movie {}", page, total, count, movie.getName());
                            source = new Source(pageUrl, movie.getSourceTime());
                            source.setMovieId(movie.getId());
                            count++;
                            total++;
                        } else {
                            source = new Source(pageUrl, false);
                        }
                        source.setCompleted(completed);
                        source.setUpdatedTime(new Date());
                        service.save(source);
                        error = 0;
                    } catch (Exception e) {
                        error++;
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("[ck-tv] Parse page failed: " + pageUrl, e);
                    }
                }

                if (doc.select(".wp-pagenavi").first().children().last().text().equals(String.valueOf(page))) {
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
                logger.error("[ck-tv] Get HTML failed: " + url, e);
            }
        }

        saveCrawlerConfig();
        savePage(1);
        logger.info("[ck-tv] ===== get {} movies =====", total);
    }

    private String getName(String text) {
        int index = text.indexOf('[');
        if (index > 0) {
            return text.substring(0, index);
        }
        return text;
    }

}
