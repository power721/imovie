package org.har01d.imovie;

import java.util.Arrays;
import java.util.List;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.domain.ResourceRepository;
import org.har01d.imovie.douban.DouBanCrawler;
import org.har01d.imovie.douban.DouBanExplorer;
import org.har01d.imovie.rs05.Rs05Crawler;
import org.har01d.imovie.util.UrlUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
    private DouBanExplorer explorer;

    @Autowired
    private ResourceRepository resourceRepository;

    public static void main(String[] args) {
        SpringApplication.run(IMovieCrawlerApplication.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
        testFindMagnet();
        testFindEd2k();

        if (!Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            rs05Crawler.crawler();
            douBanCrawler.crawler();
            explorer.crawler();
        }
    }

    private void testFindMagnet() {
        List<Resource> resources = resourceRepository.findByUriStartingWith("magnet");
        for (Resource resource : resources) {
            String uri = resource.getUri();
            String magnet = UrlUtils.findMagnet(uri);
            if (!uri.equals(magnet)) {
                System.out.println(uri + " -> " + magnet);
            }
        }
    }

    private void testFindEd2k() {
        List<Resource> resources = resourceRepository.findByUriStartingWith("ed2k");
        for (Resource resource : resources) {
            String uri = resource.getUri();
            String ed2k = UrlUtils.findED2K(uri);
            if (!uri.equals(ed2k)) {
                System.out.println(uri + " -> " + ed2k);
            }
        }
    }

}
