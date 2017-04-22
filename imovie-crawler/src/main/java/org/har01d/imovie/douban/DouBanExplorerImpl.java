package org.har01d.imovie.douban;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
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

    private BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);

    @Override
    public void crawler() throws InterruptedException {
        int total = 0;
        queue.add(baseUrl);
        while (!queue.isEmpty()) {
            String url = queue.poll(10, TimeUnit.SECONDS);
            if (url == null) {
                break;
            }

            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select(".article a[href^=" + baseUrl + "/subject/]");
                for (Element element : elements) {
                    String pageUrl = element.attr("href");
                    String title = element.text();
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
                            logger.error("Parse page failed: " + title, e);
                        }
                    }

                    explore(pageUrl, title);
                    service.save(new Source(pageUrl));
                    total++;
                }
            } catch (Exception e) {
                service.publishEvent(url, e.getMessage());
                logger.error("Get HTML failed: " + url, e);
            }
        }

        logger.info("===== get {} movies =====", total);
    }

    private void explore(String url, String title) throws InterruptedException {
        try {
            String html = HttpUtils.getHtml(url);
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("#recommendations a");
            for (Element element : elements) {
                queue.put(element.attr("href"));
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            service.publishEvent(url, e.getMessage());
            logger.error("Parse page failed: " + title, e);
        }
    }

}
