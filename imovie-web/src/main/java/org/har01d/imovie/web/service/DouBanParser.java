package org.har01d.imovie.web.service;

import java.io.IOException;
import org.har01d.imovie.web.domain.Movie;

public interface DouBanParser {

    Movie parse(Movie movie) throws IOException;

}
