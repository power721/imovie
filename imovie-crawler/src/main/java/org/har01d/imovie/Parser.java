package org.har01d.imovie;

import java.io.IOException;
import org.har01d.imovie.domain.Movie;

public interface Parser {

    Movie parse(String url, Movie movie) throws IOException;
}
