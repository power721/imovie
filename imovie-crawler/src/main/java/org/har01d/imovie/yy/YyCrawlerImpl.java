package org.har01d.imovie.yy;

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
public class YyCrawlerImpl extends AbstractCrawler implements YyCrawler {

    private static final Logger logger = LoggerFactory.getLogger(YyCrawler.class);

    @Value("${url.yy}")
    private String baseUrl;

    @Autowired
    private YyParser parser;

    private String[] types = {"", "lishi", "mohuan", "jingsong", "dushi"};

    @Override
    public void crawler() throws InterruptedException {
        work(1);
        work(2);
        work(3);
        work(4);
    }

    private void work(int id) throws InterruptedException {
        if (!checkTime(String.valueOf(id))) {
            return;
        }

        int page = getPage(String.valueOf(id));
        Config crawler = getCrawlerConfig(String.valueOf(id));
        while (true) {
            handleError();
            String url = String.format(baseUrl, types[id], page);
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                error = 0;
                Elements elements = doc.select("div.container .row .col-md-9 .row .min-height-category div a");
                if (elements.size() == 0) {
                    if (crawler != null) {
                        break;
                    }
                    crawler = saveCrawlerConfig(String.valueOf(id));
                    page = 1;
                    continue;
                }
                logger.info("[yy-{}] {}: {} movies", id, page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = element.attr("href");
                    Source source = service.findSource(pageUrl);
                    if (source != null) {
                        if (source.isCompleted()) {
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
                            logger.info("[yy-{}] {}-{}-{} find movie {} {}", id, page, total, count, movie.getName(),
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
                        logger.error("[yy-{}] Parse page failed: {}", id, pageUrl, e);
                    }
                }

                if (crawler != null && count == 0) {
                    break;
                }
                page++;
                savePage(String.valueOf(id), page);
                sleep();
            } catch (Exception e) {
                error++;
                service.publishEvent(url, e.getMessage());
                logger.error("[yy-{}] Get HTML failed: {}", id, url, e);
            }
        }

        saveCrawlerConfig(String.valueOf(id));
        savePage(String.valueOf(id), 1);
        logger.info("[yy-{}] ===== get {} movies =====", id, total);
    }

}
