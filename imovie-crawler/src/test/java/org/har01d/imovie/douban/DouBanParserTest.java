package org.har01d.imovie.douban;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class DouBanParserTest {

    @Autowired
    private DouBanParser parser;

    @Test
    public void test() throws Exception {
        parser.parse("https://movie.douban.com/subject/1298871/");
    }

    @Test
    public void testEpisode() throws Exception {
        parser.parse("https://movie.douban.com/subject/26416957/");
    }
}