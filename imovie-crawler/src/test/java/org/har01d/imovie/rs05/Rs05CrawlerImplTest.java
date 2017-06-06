package org.har01d.imovie.rs05;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class Rs05CrawlerImplTest {

    @Autowired
    private Rs05Crawler crawler;

    @Test
    public void crawler() throws Exception {
        crawler.crawler();
    }

}