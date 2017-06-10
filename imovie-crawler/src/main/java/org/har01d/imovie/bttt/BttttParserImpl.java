package org.har01d.imovie.bttt;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.douban.DouBanParser;
import org.har01d.imovie.service.MovieService;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.UrlUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BttttParserImpl implements BttttParser {

    private static final Logger logger = LoggerFactory.getLogger(BttttParser.class);

    @Value("${url.bttt.site}")
    private String baseUrl;

    @Autowired
    private DouBanParser douBanParser;

    @Autowired
    private MovieService service;

    @Override
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);

        Movie m = null;
        String dbUrl = getDbUrl(html);
        if (dbUrl != null) {
            m = service.findByDbUrl(dbUrl);
            if (m == null) {
                m = douBanParser.parse(dbUrl);
            }
        }

        if (m != null) {
            Set<Resource> resources = m.getRes();
            int size = resources.size();
            Elements elements = doc.select("#download_links .dl_item");
            for (Element element : elements) {
                String uri = baseUrl + element.select(".dl_item_cell_dld a").attr("href");
                if (uri.contains("/down/")) {
                    String title = element.select(".dl_item_cell_note").text();
                    getResource(uri, title, resources);
                }
            }

            logger.info("get {}/{} resources for movie {}", (resources.size() - size), resources.size(), m.getName());
            service.save(m);
            return m;
        }

        return null;
    }

    private String getDbUrl(String html) {
        int index = html.indexOf("movie.douban.com/subject/");
        if (index < 0) {
            return null;
        }

        String text = html.substring(index - "https://".length(), index + 45);
        Matcher matcher = UrlUtils.DB_PATTERN.matcher(text);
        if (matcher.find()) {
            String url = matcher.group(1).replace("http://", "https://");
            if (url.endsWith("/")) {
                return url;
            } else {
                return url + "/";
            }
        }
        return null;
    }

    private void getResource(String uri, String title, Set<Resource> resources) {
        try {
            String html = HttpUtils.getHtml(uri);
            Document doc = Jsoup.parse(html);
            String text = doc.select("div table a").attr("href");
            for (String magnet : UrlUtils.findMagnet(text)) {
                resources.add(service.saveResource(magnet, title));
            }

            for (String ed2k : UrlUtils.findED2K(text)) {
                resources.add(service.saveResource(ed2k, title));
            }
        } catch (IOException e) {
            logger.error("get resource failed: " + uri, e);
            service.publishEvent(uri, "get resource failed: " + uri);
        }

    }

}
