package org.har01d.imovie;

import java.util.Arrays;
import org.har01d.imovie.rs05.Rs05Crawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class ImovieCrawlerApplication implements CommandLineRunner {

    @Autowired
    private Environment environment;

    @Autowired
    private Rs05Crawler rs05Crawler;

    public static void main(String[] args) {
        SpringApplication.run(ImovieCrawlerApplication.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            rs05Crawler.crawler();
        }
    }
}
