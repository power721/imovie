package org.har01d.imovie.xyw;

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
public class XywCrawlerImpl extends AbstractCrawler implements XywCrawler {

    private static final Logger logger = LoggerFactory.getLogger(XywCrawlerImpl.class);

    @Value("${url.xyw}")
    private String baseUrl;

    @Autowired
    private XywParser parser;

//    private ScheduledExecutorService executorService;
//
//    public XywCrawlerImpl() {
//        executorService = Executors.newScheduledThreadPool(2, new MyThreadFactory("xyw"));
//    }

    @Override
    public void crawler() throws InterruptedException {
//        executorService.scheduleWithFixedDelay(() -> work("movie"), 0, 5, TimeUnit.HOURS);
//        executorService.scheduleWithFixedDelay(() -> work("tv"), 0, 6, TimeUnit.HOURS);
//        executorService.awaitTermination(3L, TimeUnit.DAYS);
//        executorService.shutdown();

        work("movie");
        work("tv");
    }

    @Override
    public boolean isNew() {
        return getCrawlerConfig("movie") == null && getCrawlerConfig("tv") == null;
    }

    private void work(String type) throws InterruptedException {
        Config crawler = getCrawlerConfig(type);
        if (!checkTime(crawler)) {
            return;
        }

        int page = getPage(type);
        while (true) {
            handleError();
            String url = baseUrl + "/" + type + "/?page=" + page;
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.row .movie-item .meta h1 a");
                if (elements.size() == 0) {
                    if (crawler != null) {
                        break;
                    }
                    crawler = saveCrawlerConfig(type);
                    page = 1;
                    continue;
                }
                logger.info("[xyw] {}: {} movies", page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = element.attr("href");
                    if (pageUrl.startsWith("/")) {
                        pageUrl = baseUrl + pageUrl;
                    }
                    if (service.findSource(pageUrl) != null) {
                        logger.info("skip {}", pageUrl);
                        continue;
                    }

                    Movie movie = new Movie();
                    movie.setTitle(element.attr("title"));
                    movie.setName(element.text());
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("[xyw] {}-{}-{} find movie {}", page, total, count, movie.getName());
                            Source source = new Source(pageUrl, movie.getSourceTime());
                            source.setMovieId(movie.getId());
                            service.save(source);
                            count++;
                            total++;
                        } else {
                            service.save(new Source(pageUrl, false));
                        }
                        error = 0;
                    } catch (Exception e) {
                        error++;
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("[xyw] Parse page failed: " + pageUrl, e);
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
                logger.error("[xyw] Get HTML failed: " + url, e);
            }
        }

        saveCrawlerConfig(type);
        savePage(type, 1);
        logger.info("[xyw] ===== {}: get {} movies =====", type, total);
    }

}
