package org.har01d.imovie;

import java.io.IOException;
import java.util.Arrays;
import org.apache.http.impl.client.BasicCookieStore;
import org.har01d.imovie.btt.BttCrawler;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.Movie;
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
    private MovieService service;

    @Autowired
    private DouBanService douBanService;

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
            updateImdbTop250();
            douBanService.tryLogin();

            new Thread(() -> {
                try {
                    rs05Crawler.crawler();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

            bttCrawler.crawler();
        }
    }

    private void updateImdbTop250() {
        Config config = service.getConfig("imdb_250");
        if (config != null) {
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
                    movie.setImdbScore(imdbScore);
                    service.save(movie);
                    count++;
                }
            }

            if (count >= 250) {
                service.saveConfig("imdb_250", "true");
            }
        } catch (IOException e) {
            logger.warn("parse page failed: " + url, e);
        }
    }
}
