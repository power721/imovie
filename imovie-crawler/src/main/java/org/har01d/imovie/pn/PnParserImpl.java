package org.har01d.imovie.pn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
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
public class PnParserImpl extends AbstractParser implements PnParser {

    private static final Logger logger = LoggerFactory.getLogger(PnParser.class);

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
            m.addResources(findResource(getId(url), movie.getName()));

            logger.info("[pn] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
            m.setSourceTime(movie.getSourceTime());
            service.save(m);
            return m;
        } else {
            findResource(getId(url), movie.getName());
        }

        logger.warn("Cannot find movie for {}: {}", movie.getName(), url);
        service.publishEvent(url, "Cannot find movie for " + movie.getName());
        return null;
    }

    private String getId(String url) {
        int index1 = url.lastIndexOf('/');
        int index2 = url.lastIndexOf('.');
        return url.substring(index1+1, index2);
    }

    private Set<Resource> findResource(String id, String name) throws IOException {

        Set<Resource> resources = new HashSet<>();
        if (skipResource) {
            return resources;
        }
        String url = "http://www.pniao.com/Mdown/ajax_downUrls/" + id;
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("X-Requested-With", "XMLHttpRequest"));
        String html = HttpUtils.getHtml(url, headers);
        Document doc = Jsoup.parse(html);

        Elements elements = doc.select("div.dUrlFlag li.dUrl_link a");
        for (Element element : elements) {
            String uri = element.attr("href");
            if (isResource(uri)) {
                String title = element.text();
                if (uri.contains("pan.baidu.com") && !title.contains("密码") && !title.contains("提取码")) {
                    String temp = element.parent().parent().text();
                    if (temp.contains("密码") || title.contains("提取码")) {
                        title = temp;
                    }
                }
                try {
                    resources
                        .add(service.saveResource(UrlUtils.convertUrl(uri), uri, TextUtils.truncate(title, 120)));
                } catch (Exception e) {
                    service.publishEvent(name, e.getMessage());
                    logger.error("[pn] get resource failed", e);
                }
            }
        }
        return resources;
    }

    private void getMovie(Document doc, Movie movie) {
        String html = doc.select("div.dbScore").html();
        if (movie.getDbUrl() == null) {
            movie.setDbUrl(UrlUtils.getDbUrl(html));
        }
    }

}
