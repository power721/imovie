package org.har01d.imovie.btapple;

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
public class BtaCrawlerImpl extends AbstractCrawler implements BtaCrawler {

    private static final Logger logger = LoggerFactory.getLogger(BtaCrawler.class);

    @Value("${url.btapple.site}")
    private String siteUrl;

    @Value("${url.btapple.page}")
    private String baseUrl;

    @Autowired
    private BtaParser parser;

    @Override
    public void crawler() throws InterruptedException {
//        ExecutorService executorService = Executors.newFixedThreadPool(2, new MyThreadFactory("BtApple"));
//        executorService.submit(() -> work(1, "movie"));
//        executorService.submit(() -> work(3, "tv"));
//        executorService.shutdown();

        work(1, "movie");
        work(3, "tv");
    }

    private void work(int id, String type) throws InterruptedException {
        Config crawler = getCrawlerConfig(type);
        if (!checkTime(crawler)) {
            return;
        }

        int page = getPage(type, 0);
        while (true) {
            handleError();
            String url = String.format(baseUrl, type, id, page);
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.row .info a");
                if (elements.size() == 0) {
                    if (crawler != null) {
                        break;
                    }
                    crawler = saveCrawlerConfig(type);
                    page = 0;
                    continue;
                }
                logger.info("[BtApple-{}] {}: {} movies", type, page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = siteUrl + element.attr("href");
                    Source source = service.findSource(pageUrl);
                    if (source != null) {
                        if (source.isCompleted() || "movie".equals(type)) {
                            continue;
                        }

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
                    movie.setTitle(element.attr("title"));
                    movie.setName(element.text());
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger
                                .info("[BtApple-{}] {}-{}-{} find movie {}", type, page, total, count, movie.getName());
                            if (source == null) {
                                source = new Source(pageUrl, movie.getSourceTime());
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
                        logger.error("[BtApple] Parse page failed: " + pageUrl, e);
                    }
                }

                if (crawler != null && count == 0) {
                    break;
                }
                page++;
                savePage(type, page);
            } catch (Exception e) {
                error++;
                service.publishEvent(url, e.getMessage());
                logger.error("[BtApple] Get HTML failed: " + url, e);
            }
        }

        saveCrawlerConfig(type);
        savePage(type, 0);
        logger.info("[BtApple] ===== {}: get {} movies =====", type, total);
    }

}
