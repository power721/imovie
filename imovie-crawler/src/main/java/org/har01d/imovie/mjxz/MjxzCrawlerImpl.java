package org.har01d.imovie.mjxz;

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
public class MjxzCrawlerImpl extends AbstractCrawler implements MjxzCrawler {

    private static final Logger logger = LoggerFactory.getLogger(MjxzCrawler.class);

    @Value("${url.mjxz.site}")
    private String siteUrl;

    @Value("${url.mjxz.page}")
    private String baseUrl;

    @Autowired
    private MjxzParser parser;

    @Override
    public void crawler() throws InterruptedException {
        if (!checkTime()) {
            return;
        }

        work("meiju");
        work("HDDY");
    }

    private void work(String type) throws InterruptedException {
        int page = getPage(type);
        Config crawler = getCrawlerConfig(type);
        while (true) {
            handleError();
            String url = String.format(baseUrl, type, page);
            if (page == 1) {
                url = url.replace("-1", "");
            }

            try {

                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                error = 0;
                Elements elements = doc.select("div.wrap ul.list li a.name");
                if (elements.size() == 0) {
                    break;
                }
                logger.info("[mjxz-{}] {}: {} movies", type, page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = siteUrl + element.attr("href");
                    Source source = service.findSource(pageUrl);
                    if (source != null) {
                        if (source.isCompleted() || "HDDY".equals(type)) {
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
                    movie.setName(getName(element));
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger
                                .info("[mjxz-{}] {}-{}-{} find movie {} {}", type, page, total, count, movie.getName(),
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
                        logger.error("[mjxz-{}] Parse page failed: {}", type, pageUrl, e);
                    }
                }

                if (!doc.select("div#pages a").last().text().equals("尾页")) {
                    crawler = saveCrawlerConfig(type);
                    page = 1;
                    continue;
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
                logger.error("[mjxz-{}] Get HTML failed: {}", type, url, e);
            }
        }

        saveCrawlerConfig(type);
        savePage(type, 1);
        logger.info("[mjxz-{}] ===== get {} movies =====", type, total);
    }

    private String getName(Element element) {
        return element.text().split("/")[0].replace("(美版)", "")
            .replace("(BBC版)", "")
            .replaceFirst("（\\d+版）", "")
            .replaceFirst("\\[.+]", "");
    }

}
