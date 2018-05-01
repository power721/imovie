package org.har01d.imovie.bt0;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.AbstractParser;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.TextUtils;
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
public class Bt0ParserImpl extends AbstractParser implements Bt0Parser {

    private static final Logger logger = LoggerFactory.getLogger(Bt0Parser.class);
    private static final Pattern pattern = Pattern.compile("var did *=(\\d+)");

    @Override
    @Transactional
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);

        getMovie(doc, movie);

        String dbUrl = movie.getDbUrl();
        Movie m = null;
        if (dbUrl != null) {
            m = getByDb(dbUrl);
        }

        if (m == null) {
            String imdb = movie.getImdbUrl();
            if (imdb != null) {
                m = service.findByImdb(imdb);
            }
        }

        if (m == null) {
            m = searchByImdb(movie);
        }

        if (m == null) {
            m = searchByName(movie);
        }

        if (m != null) {
            Set<Resource> resources = m.getRes();
            int size = resources.size();
            m.addResources(findResource(doc, movie.getName()));

            logger.info("[bt0] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
            m.setSourceTime(movie.getSourceTime());
            service.save(m);
            return m;
        } else {
            findResource(doc, movie.getName());
        }

        logger.warn("Cannot find movie for {}: {}", movie.getName(), url);
        service.publishEvent(url, "Cannot find movie for " + movie.getName());
        return null;
    }

    private Set<Resource> findResource(Document doc, String name) throws IOException {
        Set<Resource> resources = new HashSet<>();
        if (skipResource) {
            return resources;
        }

        Elements elements = doc.select("div.tabs-container ul.tabs-content div.picture-container div.container");
        for (Element element : elements) {
            String uri = element.select("a div.tag-magnet").first().parent().attr("href");
            if (isResource(uri)) {
                String title = element.select("a.torrent-title").text();
                try {
                    resources
                        .add(service.saveResource(UrlUtils.convertUrl(uri), uri, TextUtils.truncate(title, 120)));
                } catch (Exception e) {
                    service.publishEvent(name, e.getMessage());
                    logger.error("[bt0] get resource failed", e);
                }
            }
        }
        return resources;
    }

    private void getMovie(Document doc, Movie movie) {
        String html = doc.select("script").html();
        int index = html.indexOf("var did =");
        if (index > -1) {
            int index1 = html.indexOf(';', index);
            html = html.substring(index, index1);
        }
        Matcher m = pattern.matcher(html);
        if (m.matches()) {
            movie.setDbUrl("https://movie.douban.com/subject/" + m.group(1) + "/");
        }
    }

}
