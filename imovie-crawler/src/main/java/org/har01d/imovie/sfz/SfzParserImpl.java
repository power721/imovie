package org.har01d.imovie.sfz;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.har01d.imovie.AbstractParser;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.TextUtils;
import org.har01d.imovie.util.UrlUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SfzParserImpl extends AbstractParser implements SfzParser {

    private static final Logger logger = LoggerFactory.getLogger(SfzParser.class);

    @Override
    @Transactional
    public Movie parse(String url, Movie movie) throws IOException {
        String dbUrl = movie.getDbUrl();
        Movie m = null;
        if (StringUtils.isNotEmpty(dbUrl)) {
            m = getByDb(dbUrl);
        }
        if (m == null) {
            m = searchByImdb(movie);
        }

        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);

        if (m != null) {
            m.addResources(findResource(doc));

            logger
                .info("[SFZ]get {}/{} resources for movie {}", m.getNewResources(), m.getRes().size(), movie.getName());
            return service.save(m);
        } else {
            findResource(doc);
        }

        logger.warn("Cannot find movie for {}: {}", movie.getName(), url);
        service.publishEvent(url, "Cannot find movie for " + movie.getName());
        return null;
    }

    private Set<Resource> findResource(Document doc) {
        Set<Resource> resources = new HashSet<>();
        if (skipResource) {
            return resources;
        }

        for (Element element : doc.select("div.main div.detail p a")) {
            Resource resource = findResource(element);
            if (resource != null) {
                resources.add(resource);
            }
        }

        return resources;
    }

    private Resource findResource(Element element) {
        String uri = element.attr("href");
        if (!isResource(uri)) {
            return null;
        }

        String title = element.text();
        if (uri.contains("pan.baidu.com") && !title.contains("密码") && !title.contains("提取码")) {
            String temp = element.parent().text();
            if (temp.contains("密码") || title.contains("提取码")) {
                title = temp;
            }
        }

        title = TextUtils.truncate(title, 120);
        if (uri.startsWith("thunder://")) {
            return service.saveResource(UrlUtils.convertUrl(uri), uri, title);
        } else {
            return service.saveResource(uri, title);
        }
    }

}
