package org.har01d.imovie.gg;

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
public class GgParserTest {

    @Autowired
    private GgParser parser;

    @Test
    public void test() throws Exception {
        Movie movie = new Movie();
        parser.parse(
            "http://www.gagays.xyz/movie/show/8737/%E7%AA%83%E5%90%AC%E9%A3%8E%E4%BA%91+%E7%AB%8A%E8%81%BD%E9%A2%A8%E9%9B%B2+%282009%29",
            movie);
    }

}