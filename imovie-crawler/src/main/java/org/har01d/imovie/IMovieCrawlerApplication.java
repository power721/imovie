package org.har01d.imovie;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.http.impl.client.BasicCookieStore;
import org.har01d.imovie.btapple.BtaCrawler;
import org.har01d.imovie.btt.BttCrawler;
import org.har01d.imovie.bttt.BtttCrawler;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.domain.ResourceRepository;
import org.har01d.imovie.rarbt.RarBtCrawler;
import org.har01d.imovie.rs05.Rs05Crawler;
import org.har01d.imovie.service.DouBanService;
import org.har01d.imovie.service.MovieService;
import org.har01d.imovie.util.HttpUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class IMovieCrawlerApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(IMovieCrawlerApplication.class);

    @Autowired
    private Environment environment;

    @Autowired
    private Rs05Crawler rs05Crawler;

    @Autowired
    private BttCrawler bttCrawler;

    @Autowired
    private RarBtCrawler rarBtCrawler;

    @Autowired
    private BtttCrawler btttCrawler;

    @Autowired
    private BtaCrawler btaCrawler;

    @Autowired
    private MovieService service;

    @Autowired
    private DouBanService douBanService;

    @Autowired
    private ResourceRepository resourceRepository;

    public static void main(String[] args) {
        SpringApplication.run(IMovieCrawlerApplication.class, args);
    }

    @Bean
    public BasicCookieStore cookieStore() {
        return new BasicCookieStore();
    }

    @Override
    public void run(String... strings) throws Exception {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("test")) {
//            clean();
            douBanService.tryLogin();
            updateImdbTop250();

            ExecutorService executorService = Executors.newFixedThreadPool(4, new MyThreadFactory("Crawler"));
            executorService.submit(() -> {
                try {
                    rs05Crawler.crawler();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            executorService.submit(() -> {
                try {
                    rarBtCrawler.crawler();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            executorService.submit(() -> {
                try {
                    btttCrawler.crawler();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            executorService.submit(() -> {
                try {
                    btaCrawler.crawler();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            bttCrawler.crawler();
            executorService.shutdown();
        }
    }

    private void clean() {
        List<Resource> resources = resourceRepository.findByUriStartingWith("http://www.rarbt.com/");
        logger.info("find {} resources", resources.size());
        for (Resource resource : resources) {
            for (Movie movie : resource.getMovies()) {
                if (movie.getRes().remove(resource)) {
                    service.save(movie);
                    logger.info("{}: update movie {}:{}", resource.getId(), movie.getId(), movie.getName());
                }
            }
        }

        for (Resource resource : resources) {
            resourceRepository.delete(resource);
        }
    }

    private void updateImdbTop250() {
        Config config = service.getConfig("imdb_250");
        if (config != null || service.getConfig("rs05_crawler") == null) {
            return;
        }

        int count = 0;
        String url = "http://www.imdb.com/chart/top";
        try {
            String html = HttpUtils.getHtml(url);
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("table.chart tbody tr");
            for (Element element : elements) {
                String imdbUrl = "http://www.imdb.com/title/" + element.select("td.watchlistColumn div.wlb_ribbon")
                    .attr("data-tconst");
                String imdbScore = element.select("td.imdbRating strong").text();
                Movie movie = service.findByImdb(imdbUrl);
                if (movie != null) {
                    logger.info("update imdb for movie {}", movie.getName());
                    movie.setImdbScore(imdbScore);
                    service.save(movie);
                    count++;
                }
            }

            logger.info("update {} movies for imdb", count);
            if (count >= 250) {
                service.saveConfig("imdb_250", "true");
            }
        } catch (IOException e) {
            logger.warn("parse page failed: " + url, e);
        }
    }
}
