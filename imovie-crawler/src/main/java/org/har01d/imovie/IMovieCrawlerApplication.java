package org.har01d.imovie;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
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
import org.har01d.imovie.dyb.DybCrawler;
import org.har01d.imovie.fix.FixCrawler;
import org.har01d.imovie.gg.GgCrawler;
import org.har01d.imovie.hqc.HqcCrawler;
import org.har01d.imovie.ihd.IhdCrawler;
import org.har01d.imovie.imdb.ImdbCrawler;
import org.har01d.imovie.inp.InpCrawler;
import org.har01d.imovie.lg.LgCrawler;
import org.har01d.imovie.lyw.LywCrawler;
import org.har01d.imovie.mjtt.MjttCrawler;
import org.har01d.imovie.mjxz.MjxzCrawler;
import org.har01d.imovie.mp4.Mp4Crawler;
import org.har01d.imovie.pn.PnCrawler;
import org.har01d.imovie.rarbt.RarBtCrawler;
import org.har01d.imovie.rs05.Rs05Crawler;
import org.har01d.imovie.s80.S80Crawler;
import org.har01d.imovie.service.DouBanService;
import org.har01d.imovie.service.MovieService;
import org.har01d.imovie.sfz.SfzCrawler;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.xyw.XywCrawler;
import org.har01d.imovie.yy.YyCrawler;
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
    private Mp4Crawler mp4Crawler;

    @Autowired
    private InpCrawler inpCrawler;

    @Autowired
    private CkCrawler ckCrawler;

    @Autowired
    private CkDramaCrawler ckDramaCrawler;

    @Autowired
    private MjxzCrawler mjxzCrawler;

    @Autowired
    private DybCrawler dybCrawler;

    @Autowired
    private YyCrawler yyCrawler;

    @Autowired
    private HqcCrawler hqcCrawler;

    @Autowired
    private GgCrawler ggCrawler;

    @Autowired
    private MjttCrawler mjttCrawler;

    @Autowired
    private LywCrawler lywCrawler;

    @Autowired
    private S80Crawler s80Crawler;

    @Autowired
    private IhdCrawler ihdCrawler;

    @Autowired
    private SfzCrawler sfzCrawler;

    @Autowired
    private PnCrawler pnCrawler;

    @Autowired
    private DouBanCrawler douBanCrawler;

    @Autowired
    private MovieService service;

    @Autowired
    private DouBanService douBanService;

    @Autowired
    private ImdbRepository imdbRepository;

    @Value("${crawlers:all}")
    private String type;

    @Value("${fix:0}")
    private int fix = 0;

    @Value("${offset:0}")
    private int offset = 0;

    @Value("${threads:1}")
    private int threads = 1;
    private ScheduledExecutorService executorServiceOld;
    private ScheduledExecutorService executorServiceNew;

    public static void main(String[] args) {
        SpringApplication.run(IMovieCrawlerApplication.class, args);
    }

    @Bean
    public BasicCookieStore cookieStore() {
        return new BasicCookieStore();
    }

    @Override
    public void run(String... strings) throws Exception {
        if (!environment.acceptsProfiles("test")) {
            if (fix > 0) {
                service.fixDuplicateResources(offset, fix);
                return;
            }

            Set<String> types = new HashSet<>();
            types.addAll(Arrays.asList(type.split(",")));
            douBanService.tryLogin();
//            updateImdbTop250();
            service.fixDuplicateMovies();

            logger.info("Threads: {}", threads);
            ScheduledExecutorService executorService;
            executorServiceOld = Executors.newSingleThreadScheduledExecutor();
            executorServiceNew = Executors.newScheduledThreadPool(threads, new MyThreadFactory("Crawler"));

            if (types.contains("all") || types.contains("rar")) {
                scheduleCrawler(rarBtCrawler, 5);
            }

            if (types.contains("all") || types.contains("rs05")) {
                scheduleCrawler(rs05Crawler, 5);
            }

            if (types.contains("all") || types.contains("bttt")) {
                scheduleCrawler(btttCrawler, 5);
            }

            if (types.contains("all") || types.contains("bta")) {
                scheduleCrawler(btaCrawler, 5);
            }

            if (types.contains("all") || types.contains("btdy")) {
                scheduleCrawler(btdyCrawler, 5);
            }

            if (types.contains("all") || types.contains("btp")) {
                scheduleCrawler(btPanCrawler, 5);
            }

            if (types.contains("all") || types.contains("fix")) {
                scheduleCrawler(fixCrawler, 6);
            }

            if (types.contains("all") || types.contains("zmz")) {
                scheduleCrawler(zmzCrawler, 4);
            }

            if (types.contains("all") || types.contains("btxf")) {
                scheduleCrawler(btxfCrawler, 6);
            }

            if (types.contains("all") || types.contains("lg")) {
                scheduleCrawler(lgCrawler, 6);
            }

            if (types.contains("all") || types.contains("ck")) {
                scheduleCrawler(ckCrawler, 6);
            }

//            if (types.contains("all") || types.contains("mp4")) {
//                scheduleCrawler(mp4Crawler, 6);
//            }

            if (types.contains("all") || types.contains("inp")) {
                scheduleCrawler(inpCrawler, 6);
            }

            if (types.contains("all") || types.contains("mjxz")) {
                scheduleCrawler(mjxzCrawler, 6);
            }

            if (types.contains("all") || types.contains("dyb")) {
                scheduleCrawler(dybCrawler, 6);
            }

            if (types.contains("all") || types.contains("yy")) {
                scheduleCrawler(yyCrawler, 6);
            }

            if (types.contains("all") || types.contains("hqc")) {
                scheduleCrawler(hqcCrawler, 6);
            }

            if (types.contains("all") || types.contains("xyw")) {
                scheduleCrawler(xywCrawler, 6);
            }

            if (types.contains("all") || types.contains("gg")) {
                scheduleCrawler(ggCrawler, 6);
            }

            if (types.contains("all") || types.contains("mjtt")) {
                scheduleCrawler(mjttCrawler, 6);
            }

            if (types.contains("all") || types.contains("lyw")) {
                scheduleCrawler(lywCrawler, 6);
            }

            if (types.contains("all") || types.contains("s80")) {
                scheduleCrawler(s80Crawler, 6);
            }

            if (types.contains("all") || types.contains("ihd")) {
                scheduleCrawler(ihdCrawler, 6);
            }

            if (types.contains("all") || types.contains("sfz")) {
                scheduleCrawler(sfzCrawler, 6);
            }

            if (types.contains("all") || types.contains("pn")) {
                scheduleCrawler(pnCrawler, 6);
            }

            if (types.contains("ckd")) {
                ckDramaCrawler.crawler();
            }

            if (types.contains("all") || types.contains("imdb")) {
                executorService = imdbCrawler.isNew() ? executorServiceNew : executorServiceOld;
                executorService.submit(() -> {
                    try {
                        imdbCrawler.crawler();
                        updateImdb();
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                });
            }

            if (types.contains("douban")) {
                douBanCrawler.crawler();
            }

            if (types.contains("all") || types.contains("btt")) {
                scheduleCrawler(bttCrawler, 6);
            }
        }
    }

    private void scheduleCrawler(Crawler crawler, int delay) {
        if (crawler.isNew()) {
            delay /= 2;
        }
        ScheduledExecutorService executorService = crawler.isNew() ? executorServiceNew : executorServiceOld;
        executorService.scheduleWithFixedDelay(() -> {
            try {
                crawler.crawler();
            } catch (Exception e) {
                logger.error("crawler failed", e);
            }
        }, 0, delay, TimeUnit.HOURS);
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
