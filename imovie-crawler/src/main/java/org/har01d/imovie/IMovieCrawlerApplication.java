package org.har01d.imovie;

import java.util.Arrays;
import org.apache.http.impl.client.BasicCookieStore;
import org.har01d.imovie.btt.BttCrawler;
import org.har01d.imovie.douban.DouBanCrawler;
import org.har01d.imovie.douban.DouBanExplorer;
import org.har01d.imovie.rs05.Rs05Crawler;
import org.har01d.imovie.service.DouBanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class IMovieCrawlerApplication implements CommandLineRunner {

    @Autowired
    private Environment environment;

    @Autowired
    private Rs05Crawler rs05Crawler;

    @Autowired
    private DouBanCrawler douBanCrawler;

    @Autowired
    private BttCrawler bttCrawler;

    @Autowired
    private DouBanExplorer explorer;

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
            douBanService.tryLogin();

            rs05Crawler.crawler();

//            new Thread(() -> {
//                try {
//                    explorer.crawler();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }).start();
//
            bttCrawler.crawler();
//            douBanCrawler.crawler();
        }
    }

}
