package org.har01d.imovie.douban;

import java.io.IOException;
import java.util.List;
import org.har01d.imovie.domain.Movie;

public interface DouBanParser {

    Movie parse(String url) throws IOException;

    Movie parse(String url, boolean includeDbList) throws IOException;

    List<Movie> search(String text) throws IOException;
}
