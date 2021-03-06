package org.har01d.imovie.btpan;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.har01d.imovie.AbstractParser;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.util.HttpUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BtPanParserImpl extends AbstractParser implements BtPanParser {

    private static final Logger logger = LoggerFactory.getLogger(BtPanParser.class);

    @Value("${url.btpan}")
    private String baseUrl;

    @Override
    @Transactional
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);

        String dbUrl = movie.getDbUrl();
        Movie m = getByDb(dbUrl);

        if (m != null) {
            Set<Resource> resources = m.getRes();
            int size = resources.size();
            m.addResources(getResource(doc));

            logger.info("[btpan] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
            service.save(m);
            return m;
        } else {
            getResource(doc);
        }

        logger.warn("Cannot find movie for {}-{}: {}", movie.getName(), movie.getTitle(), url);
        service.publishEvent(url, "Cannot find movie for " + movie.getName() + " - " + movie.getTitle());
        return null;
    }

    private Set<Resource> getResource(Document doc) {
        Set<Resource> resources = new HashSet<>();
        if (skipResource) {
            return resources;
        }

        Elements elements = doc.select("div.download ul li");
        for (Element element : elements) {
            String uri = element.select("span a").attr("href");
            if (isResource(uri)) {
                String original = baseUrl + element.select("a").attr("href");
                String title = element.select("a").first().text();
                resources.add(service.saveResource(uri, original, title));
            }
        }
        return resources;
    }

}
