package org.har01d.imovie;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.http.impl.client.BasicCookieStore;
import org.har01d.imovie.btapple.BtaCrawler;
import org.har01d.imovie.btdy.BtdyCrawler;
import org.har01d.imovie.btpan.BtPanCrawler;
import org.har01d.imovie.btt.BttCrawler;
import org.har01d.imovie.bttt.BtttCrawler;
import org.har01d.imovie.btxf.BtxfCrawler;
import org.har01d.imovie.ck.CkCrawler;
import org.har01d.imovie.ck.CkDramaCrawler;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.Imdb;
import org.har01d.imovie.domain.ImdbRepository;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.douban.DouBanCrawler;
import org.har01d.imovie.fix.FixCrawler;
import org.har01d.imovie.imdb.ImdbCrawler;
import org.har01d.imovie.lg.LgCrawler;
import org.har01d.imovie.rarbt.RarBtCrawler;
import org.har01d.imovie.rs05.Rs05Crawler;
import org.har01d.imovie.service.DouBanService;
import org.har01d.imovie.service.MovieService;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.xyw.XywCrawler;
import org.har01d.imovie.zmz.ZmzCrawler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private BtdyCrawler btdyCrawler;

    @Autowired
    private BtPanCrawler btPanCrawler;

    @Autowired
    private XywCrawler xywCrawler;

    @Autowired
    private ImdbCrawler imdbCrawler;

    @Autowired
    private FixCrawler fixCrawler;

    @Autowired
    private ZmzCrawler zmzCrawler;

    @Autowired
    private BtxfCrawler btxfCrawler;

    @Autowired
    private LgCrawler lgCrawler;

    @Autowired
    private CkCrawler ckCrawler;

    @Autowired
    private CkDramaCrawler ckDramaCrawler;

    @Autowired
    private DouBanCrawler douBanCrawler;

    @Autowired
    private MovieService service;

    @Autowired
    private DouBanService douBanService;

    @Autowired
    private ImdbRepository imdbRepository;

    @Value("${crawlers:all}")
    private Set<String> types;

    @Value("${fix:0}")
    private int fix = 0;

    @Value("${offset:0}")
    private int offset = 0;

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
            if (fix > 0) {
                service.fixDuplicateResources(offset, fix);
                return;
            }

            douBanService.tryLogin();
//            updateImdbTop250();
            service.fixDuplicateMovies();

            ScheduledExecutorService executorService = Executors
                .newScheduledThreadPool(5, new MyThreadFactory("Crawler"));

//            executorService.scheduleWithFixedDelay(() -> {
//                try {
//                    rarBtCrawler.crawler();
//                } catch (Exception e) {
//                    logger.error("", e);
//                }
//            }, 0, 5, TimeUnit.HOURS);

            if (types.contains("all") || types.contains("rs05")) {
                executorService.scheduleWithFixedDelay(() -> {
                    try {
                        rs05Crawler.crawler();
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }, 0, 3, TimeUnit.HOURS);
            }

            if (types.contains("all") || types.contains("bttt")) {
                executorService.scheduleWithFixedDelay(() -> {
                    try {
                        btttCrawler.crawler();
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }, 0, 5, TimeUnit.HOURS);
            }

            if (types.contains("all") || types.contains("bta")) {
                executorService.scheduleWithFixedDelay(() -> {
                    try {
                        btaCrawler.crawler();
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }, 0, 5, TimeUnit.HOURS);
            }

            if (types.contains("all") || types.contains("btdy")) {
                executorService.scheduleWithFixedDelay(() -> {
                    try {
                        btdyCrawler.crawler();
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }, 0, 5, TimeUnit.HOURS);
            }

            if (types.contains("all") || types.contains("btp")) {
                executorService.scheduleWithFixedDelay(() -> {
                    try {
                        btPanCrawler.crawler();
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }, 0, 5, TimeUnit.HOURS);
            }

            if (types.contains("all") || types.contains("xyw")) {
                executorService.submit(() -> {
                    try {
                        xywCrawler.crawler();
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                });
            }

            if (types.contains("all") || types.contains("imdb")) {
                executorService.submit(() -> {
                    try {
                        imdbCrawler.crawler();
                        updateImdb();
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                });
            }

            if (types.contains("all") || types.contains("fix")) {
                executorService.scheduleWithFixedDelay(() -> {
                    try {
                        fixCrawler.crawler();
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }, 0, 6, TimeUnit.HOURS);
            }

            if (types.contains("all") || types.contains("zmz")) {
                executorService.scheduleWithFixedDelay(() -> {
                    try {
                        zmzCrawler.crawler();
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }, 0, 1, TimeUnit.HOURS);
            }

            if (types.contains("all") || types.contains("btxf")) {
                executorService.scheduleWithFixedDelay(() -> {
                    try {
                        btxfCrawler.crawler();
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }, 0, 6, TimeUnit.HOURS);
            }

            if (types.contains("all") || types.contains("lg")) {
                executorService.scheduleWithFixedDelay(() -> {
                    try {
                        lgCrawler.crawler();
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }, 0, 6, TimeUnit.HOURS);
            }

            if (types.contains("all") || types.contains("ck")) {
                executorService.scheduleWithFixedDelay(() -> {
                    try {
                        ckCrawler.crawler();
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }, 0, 6, TimeUnit.HOURS);
            }

            if (types.contains("ckd")) {
                ckDramaCrawler.crawler();
            }

            if (types.contains("douban")) {
                douBanCrawler.crawler();
            }

            if (types.contains("all") || types.contains("btt")) {
                bttCrawler.crawler();
            }
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

    private void updateImdb() {
        if (service.getConfig("imdb") != null) {
            return;
        }

        List<Imdb> imdbList = imdbRepository.findAll();
        for (Imdb imdb : imdbList) {
            Movie movie = service.findByImdb("http://www.imdb.com/title/" + imdb.getId());
            if (movie != null) {
                movie.setImdbScore(imdb.getRating());
                service.save(movie, false);
            }
        }

        service.saveConfig("imdb", new Date().toString());
    }

}
