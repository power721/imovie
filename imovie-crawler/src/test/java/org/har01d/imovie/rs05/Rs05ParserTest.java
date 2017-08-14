package org.har01d.imovie.rs05;

import org.har01d.imovie.domain.Movie;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
@Ignore
public class Rs05ParserTest {

    @Autowired
    private Rs05Parser parser;

    @Test
    public void test() throws Exception {
        String pageUrl = "http://www.rs05.com/movie6418.html";
        Movie movie = new Movie();
        parser.parse(pageUrl, movie);
    }

    @Test
    public void testBaidu() throws Exception {
        String pageUrl = "http://www.rs05.com/movie118349.html";
        Movie movie = new Movie();
        parser.parse(pageUrl, movie);
    }

    @Test
    public void testChina() throws Exception {
        String pageUrl = "http://www.rs05.com/movie6867.html";
        Movie movie = new Movie();
        parser.parse(pageUrl, movie);
    }

    @Test
    public void testMissingField() throws Exception {
        String pageUrl = "http://www.rs05.com/movie6141.html";
        Movie movie = new Movie();
        parser.parse(pageUrl, movie);
    }

    @Test
    public void testOld() throws Exception {
        String pageUrl = "http://www.rs05.com/movie100087.html";
        Movie movie = new Movie();
        parser.parse(pageUrl, movie);
    }
}