package org.har01d.imovie.douban;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import org.apache.http.client.HttpResponseException;
import org.apache.http.impl.client.BasicCookieStore;
import org.har01d.imovie.MyThreadFactory;
import org.har01d.imovie.domain.Explorer;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Source;
import org.har01d.imovie.service.MovieService;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.UrlUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DouBanExplorerImpl implements DouBanExplorer {

    private static final Logger logger = LoggerFactory.getLogger(DouBanExplorer.class);
    private static final String TYPE = "db";

    @Value("${url.douban}")
    private String baseUrl;

    @Autowired
    private DouBanParser parser;

    @Autowired
    private MovieService service;

    @Autowired
    private BasicCookieStore cookieStore;

    private BlockingQueue<Explorer> queue = new ArrayBlockingQueue<>(10);

    private AtomicInteger count = new AtomicInteger();
    private ExecutorService executorService;
    private ExecutorService exploreService;

    @Override
    public void crawler() throws InterruptedException {
        executorService = Executors.newSingleThreadExecutor(new MyThreadFactory("DBExplorer-C"));
        exploreService = Executors.newSingleThreadExecutor(new MyThreadFactory("DBExplorer-P"));

        logger.info("explore");
        executorService.submit(() -> {
            try {
                explore();
            } catch (InterruptedException e) {
                // ignore
            }
        });

        int page = 0;
        while (true) {
            Pageable pageable = new PageRequest(page, 20);
            Page<Explorer> explorers = service.findExplorers(TYPE, pageable);
            for (Explorer explorer : explorers) {
                queue.put(explorer);
            }

            if (explorers.hasNext()) {
                page = 1 - page;
            } else {
                break;
            }
        }

        JSONParser jsonParser = new JSONParser();
        if (queue.isEmpty()) {
            String url = baseUrl + "/j/search_subjects?tag=%E7%83%AD%E9%97%A8&sort=time&page_limit=500";
            try {
                String json = HttpUtils.getJson(url, cookieStore);
                JSONObject jsonObject = (JSONObject) jsonParser.parse(json);
                JSONArray items = (JSONArray) jsonObject.get("subjects");
                for (Object item1 : items) {
                    JSONObject item = (JSONObject) item1;
                    String pageUrl = getDbUrl((String) item.get("url"));
                    if (service.findSource(pageUrl) == null) {
                        queue.put(service.save(new Explorer(TYPE, pageUrl)));
                    }
                }
            } catch (Exception e) {
                service.publishEvent(url, e.getMessage());
                logger.error("Parse page failed: " + url, e);
            }
        }
    }

    private void explore() throws InterruptedException {
        while (true) {
            Explorer explorer = queue.poll(30, TimeUnit.SECONDS);
            if (explorer == null) {
                break;
            }

            String pageUrl = explorer.getUri();
            if (service.findSource(pageUrl) != null) {
                service.delete(explorer);
                continue;
            }

            Movie movie = service.findByDbUrl(pageUrl);
            if (movie == null) {
                try {
                    movie = parser.parse(pageUrl);
                    service.save(movie);
                    service.save(new Source(pageUrl));
                    logger.info("{}: find movie {}", count.incrementAndGet(), movie.getTitle());
                } catch (HttpResponseException e) {
                    service.publishEvent(pageUrl, e.getMessage());
                    logger.error("Parse page failed: " + pageUrl, e);
                    if (e.getStatusCode() == 404) {
                        service.delete(explorer);
                        service.save(new Source(pageUrl));
                        continue;
                    }
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
        }

        logger.info("===== get {} movies =====", count.get());
        exploreService.shutdown();
        executorService.shutdown();
        exploreService.shutdownNow();
        executorService.shutdownNow();
    }

    private void explore(String url) throws InterruptedException {
        try {
            String html = HttpUtils.getHtml(url, "UTF-8", cookieStore);
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("#recommendations a");
            for (Element element : elements) {
                String pageUrl = getDbUrl(element.attr("href"));
                if (service.findSource(pageUrl) == null) {
                    queue.put(service.save(new Explorer(TYPE, pageUrl)));
                }
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            service.publishEvent(url, e.getMessage());
            logger.error("Parse page failed: " + url, e);
        }
    }

    private String getDbUrl(String text) {
        Matcher matcher = UrlUtils.DB_PATTERN.matcher(text);
        if (matcher.find()) {
            String url = matcher.group(1).replace("http://", "https://");
            if (url.endsWith("/")) {
                return url;
            } else {
                return url + "/";
            }
        }
        return text;
    }

}
