package org.har01d.imovie.ihd;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.har01d.imovie.AbstractParser;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.UrlUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IhdParserImpl extends AbstractParser implements IhdParser {

    private static final Logger logger = LoggerFactory.getLogger(IhdParser.class);

    @Override
    @Transactional
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);

        String dbUrl = UrlUtils.getDbUrl(doc.select("table.score-intro th").html());
        Movie m = getByDb(dbUrl);

        if (m != null) {
            Set<Resource> resources = m.getRes();
            m.addResources(getResource(doc));

            logger.info("[IHD] get {}/{} resources for movie {}", m.getNewResources(), resources.size(),
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

        Elements elements = doc.select("div#dwn table tr");
        logger.info("[IHD] Resources-{}", elements.size());
        for (Element element : elements) {
            String title = element.select("td").first().text();
            String uri = element.select("td.t_dwn a").first().attr("href");
            if (isResource(uri)) {
                resources.add(service.saveResource(uri, title));
            }
        }
        return resources;
    }

}
