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
        String pageUrl = "http://btbtt.co/thread-index-fid-951-tid-4351512.htm";
        Movie movie = new Movie();
        movie.setTitle("test");
        parser.parse(pageUrl, movie);
    }

    @Test
    public void test2() throws Exception {
        String pageUrl = "http://btbtt.co/thread-index-fid-1183-tid-4172428.htm";
        Movie movie = new Movie();
        movie.setTitle("test");
        parser.parse(pageUrl, movie);
    }

    @Test
    public void test3() throws Exception {
        String pageUrl = "http://btbtt.co/thread-index-fid-1183-tid-4172432.htm";
        Movie movie = new Movie();
        movie.setTitle("test");
        parser.parse(pageUrl, movie);
    }

    @Test
    public void test4() throws Exception {
        String pageUrl = "http://btbtt.co/thread-index-fid-1183-tid-4172508.htm";
        Movie movie = new Movie();
        movie.setTitle("test");
        parser.parse(pageUrl, movie);
    }

    @Test
    public void test5() throws Exception {
        String pageUrl = "http://btbtt.co/thread-index-fid-1183-tid-4258696.htm";
        Movie movie = new Movie();
        movie.setTitle("test");
        parser.parse(pageUrl, movie);
    }

    @Test
    public void testParseDbStyle() throws Exception {
        String pageUrl = "http://btbtt.co/thread-index-fid-951-tid-4353025.htm";
        Movie movie = new Movie();
        movie.setTitle("test");
        parser.parse(pageUrl, movie);
    }

    @Test
    public void testDbUrl() throws Exception {
        String pageUrl = "http://btbtt.co/thread-index-fid-951-tid-4351504.htm";
        Movie movie = new Movie();
        movie.setTitle("test");
        movie = parser.parse(pageUrl, movie);
//        assertEquals("https://movie.douban.com/subject/1584991/", movie.getDbUrl());
    }

    @Test
    public void testImdbUrl() throws Exception {
        String pageUrl = "http://btbtt.co/thread-index-fid-951-tid-4351478.htm";
        Movie movie = new Movie();
        movie.setTitle("test");
        movie = parser.parse(pageUrl, movie);
//        assertEquals("http://www.imdb.com/title/tt0844286", movie.getImdbUrl());
    }

    @Test
    public void testBaidu() throws Exception {
        String pageUrl = "http://btbtt.co/thread-index-fid-951-tid-4350681.htm";
        Movie movie = new Movie();
        movie.setTitle("test");
        parser.parse(pageUrl, movie);
    }

}