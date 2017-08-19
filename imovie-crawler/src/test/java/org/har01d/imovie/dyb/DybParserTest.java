package org.har01d.imovie.dyb;

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
public class DybParserTest {

    @Autowired
    private DybParser parser;

    @Test
    public void test() throws Exception {
        Movie movie = new Movie();
        parser.parse("http://www.dybird.com/down/xiazai49131.html", movie);
    }

}