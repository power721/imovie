package org.har01d.imovie.btt;

import java.io.IOException;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.util.HttpUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BttParserImpl implements BttParser {

    private static final Logger logger = LoggerFactory.getLogger(BttParser.class);

    @Override
    public void parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);
        Elements elements = doc.select("div.post p");

        for (Element element : elements) {
            logger.debug(element.tagName());
        }
    }
}
