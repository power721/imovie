package org.har01d.imovie.btt;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class BttCrawlerTest {

    @Autowired
    private BttCrawler crawler;

//    @Test
//    public void test() throws Exception {
//        crawler.crawler();
//    }
}