package org.har01d.imovie.mj;

import java.util.Date;
import java.util.Random;
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
public class MjCrawlerImpl extends AbstractCrawler implements MjCrawler {

    private static final Logger logger = LoggerFactory.getLogger(MjCrawler.class);

    @Value("${url.mj.site}")
    private String siteUrl;

    @Value("${url.mj.page}")
    private String baseUrl;

    @Autowired
    private MjParser parser;

    private Random random = new Random();

    @Override
    public void crawler() throws InterruptedException {
        work("meiju");
        work("HDDY");
    }

    private void work(String type) {
        int total = 0;
        int error = 0;
        int page = getPage(type);
        Config crawler = getCrawlerConfig(type);
        while (true) {
            String url = String.format(baseUrl, type, page);
            if (page == 1) {
                url = url.replace("-1", "");
            }

            try {
                if (error >= 5) {
                    logger.warn("sleep {} seconds", error * 30L);
                    TimeUnit.SECONDS.sleep(error * 30L);
                }

                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.wrap ul.list li a.name");
                if (elements.size() == 0) {
                    crawler = saveCrawlerConfig(type);
                    page = 1;
                    continue;
                }
                logger.info("[mj-{}] {}: {} movies", type, page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = siteUrl + element.attr("href");
                    Source source = service.findSource(pageUrl);
                    if (source != null) {
                        long time = System.currentTimeMillis();
                        if ((time - source.getUpdatedTime().getTime()) < TimeUnit.HOURS.toMillis(12)) {
                            logger.info("skip {}", pageUrl);
                            continue;
                        }
                    }

                    Movie movie = new Movie();
                    if (source != null && source.getMovieId() != null) {
                        movie.setId(source.getMovieId());
                    }
                    movie.setName(getName(element));
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("[mj] {}-{}-{} find movie {} {}", page, total, count, movie.getName(), pageUrl);
                            if (source == null) {
                                source = new Source(pageUrl, movie.isCompleted());
                            }
                            source.setMovieId(movie.getId());
                            count++;
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
                        logger.error("[mj-{}] Parse page failed: {}", type, pageUrl, e);
                    }
                }

                if (crawler != null && count == 0) {
                    break;
                }
                page++;
                savePage(type, page);
                TimeUnit.MILLISECONDS.sleep(random.nextInt(500));
            } catch (Exception e) {
                error++;
                service.publishEvent(url, e.getMessage());
                logger.error("[mj-{}] Get HTML failed: {}", type, url, e);
            }
        }

        saveCrawlerConfig(type);
        savePage(type, 1);
        logger.info("[mj-{}] ===== get {} movies =====", type, total);
    }

    private String getName(Element element) {
        return element.text().split("/")[0];
    }

}
