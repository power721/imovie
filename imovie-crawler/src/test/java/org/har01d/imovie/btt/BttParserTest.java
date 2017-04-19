package org.har01d.imovie.btt;

import org.har01d.imovie.domain.Movie;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class BttParserTest {

    @Autowired
    private BttParser parser;

    @Test
    public void test() throws Exception {
        String pageUrl = "http://btbtt.co/thread-index-fid-951-tid-4350535.htm";
        Movie movie = new Movie();
        movie.setTitle("test");
        parser.parse(pageUrl, movie);
    }

    @Test
    public void testBaidu() throws Exception {
        String pageUrl = "http://btbtt.co/thread-index-fid-951-tid-4350681.htm";
        Movie movie = new Movie();
        movie.setTitle("test");
        parser.parse(pageUrl, movie);
    }

}