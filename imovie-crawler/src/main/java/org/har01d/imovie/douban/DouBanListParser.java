package org.har01d.imovie.douban;

import java.io.IOException;
import java.text.ParseException;

public interface DouBanListParser {

    boolean parse(String url) throws IOException, ParseException;
}
