package org.har01d.imovie.hqc;

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
public class HqcCrawlerImpl extends AbstractCrawler implements HqcCrawler {

    private static final Logger logger = LoggerFactory.getLogger(HqcCrawler.class);

    @Value("${url.hqc.site}")
    private String siteUrl;

    @Value("${url.hqc.page}")
    private String baseUrl;

    @Autowired
    private HqcParser parser;

    private Random random = new Random();

    @Override
    public void crawler() throws InterruptedException {
        int page = getPage();
        Config crawler = getCrawlerConfig();
        while (true) {
            handleError();
            String url = String.format(baseUrl, page);
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("table.threadlist tr.thread");
                if (elements.size() == 0) {
                    if (crawler != null) {
                        break;
                    }
                    crawler = saveCrawlerConfig();
                    page = 1;
                    continue;
                }
                logger.info("[HQC] {}: {} movies", page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = siteUrl + element.attr("href");
                    String type = element.select("td.td-subject .subject a").first().text();
                    if (type.contains("[GBT游戏]")) {
                        continue;
                    }

                    Source source = service.findSource(pageUrl);
                    if (source != null) {
                        if (source.isCompleted() || type.contains("[HQC电影]")) {
                            continue;
                        }

                        long time = System.currentTimeMillis();
                        if ((time - source.getUpdatedTime().getTime()) < TimeUnit.HOURS.toMillis(24)) {
                            logger.info("skip {}", pageUrl);
                            continue;
                        }
                    }

                    Movie movie = new Movie();
                    if (source != null && source.getMovieId() != null) {
                        movie.setId(source.getMovieId());
                    }
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("[HQC] {}-{}-{} find movie {} {}", page, total, count, movie.getName(),
                                pageUrl);
                            if (source == null) {
                                source = new Source(pageUrl, movie.isCompleted());
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
                        logger.error("[HQC] Parse page failed: {}", pageUrl, e);
                    }
                }

                if (crawler != null && count == 0) {
                    break;
                }
                page++;
                savePage(page);
                TimeUnit.MILLISECONDS.sleep(random.nextInt(500));
            } catch (Exception e) {
                error++;
                service.publishEvent(url, e.getMessage());
                logger.error("[HQC] Get HTML failed: {}", url, e);
            }
        }

        saveCrawlerConfig();
        savePage(1);
        logger.info("[HQC] ===== get {} movies =====", total);
    }

}
