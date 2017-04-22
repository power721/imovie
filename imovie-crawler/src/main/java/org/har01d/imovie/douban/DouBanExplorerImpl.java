package org.har01d.imovie.douban;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.har01d.imovie.domain.Explorer;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Source;
import org.har01d.imovie.service.MovieService;
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
public class DouBanExplorerImpl implements DouBanExplorer {

    private static final Logger logger = LoggerFactory.getLogger(DouBanExplorer.class);

    @Value("${url.douban}")
    private String baseUrl;

    @Autowired
    private DouBanParser parser;

    @Autowired
    private MovieService service;

    private BlockingQueue<Explorer> queue = new ArrayBlockingQueue<>(10);

    private ExecutorService executorService;
    private ExecutorService exploreService;

    @Override
    public void crawler() throws InterruptedException {
        executorService = Executors.newSingleThreadExecutor();
        exploreService = Executors.newSingleThreadExecutor();

        for (Explorer explorer : service.findExplorers("db")) {
            queue.put(explorer);
        }

        if (queue.isEmpty()) {
            try {
                String html = HttpUtils.getHtml(baseUrl);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select(".article a[href^=" + baseUrl + "/subject/]");
                for (Element element : elements) {
                    String pageUrl = element.attr("href");
                    if (service.findSource(pageUrl) == null) {
                        queue.put(service.save(new Explorer("db", pageUrl)));
                    }
                }
            } catch (Exception e) {
                service.publishEvent(baseUrl, e.getMessage());
                logger.error("Get HTML failed: " + baseUrl, e);
            }
        }

        executorService.submit(() -> {
            try {
                explore();
            } catch (InterruptedException e) {
                // ignore
            }
        });

    }

    private void explore() throws InterruptedException {
        int total = 0;
        while (!queue.isEmpty()) {
            Explorer explorer = queue.poll(10, TimeUnit.SECONDS);
            if (explorer == null) {
                break;
            }

            String pageUrl = explorer.getUri();
            if (service.findSource(pageUrl) != null) {
                continue;
            }

            Movie movie = service.find(pageUrl);
            if (movie == null) {
                try {
                    movie = parser.parse(pageUrl);
                    service.save(movie);
                } catch (Exception e) {
                    service.publishEvent(pageUrl, e.getMessage());
                    logger.error("Parse page failed: " + pageUrl, e);
                }
            }

            exploreService.submit(() -> {
                try {
                    explore(pageUrl);
                } catch (InterruptedException e) {
                    // ignore
                }
                service.delete(explorer);
                service.save(new Source(pageUrl));
            });
            total++;
        }

        logger.info("===== get {} movies =====", total);
    }

    private void explore(String url) throws InterruptedException {
        try {
            String html = HttpUtils.getHtml(url);
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("#recommendations a");
            for (Element element : elements) {
                queue.put(service.save(new Explorer("db", element.attr("href"))));
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            service.publishEvent(url, e.getMessage());
            logger.error("Parse page failed: " + url, e);
        }
    }

}
