package org.har01d.imovie.douban;

import java.io.IOException;
import org.har01d.imovie.domain.Movie;

public interface DouBanParser {
    Movie parse(String url) throws IOException;
}
