package org.har01d.imovie.lg;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.har01d.imovie.AbstractParser;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.StringUtils;
import org.har01d.imovie.util.UrlUtils;
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
public class LgParserImpl extends AbstractParser implements LgParser {

    private static final Logger logger = LoggerFactory.getLogger(LgParser.class);

    @Value("${url.lg}")
    private String baseUrl;

    @Override
    @Transactional
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);

        getMovie(doc, movie);

        String dbUrl = movie.getDbUrl();
        Movie m = getByDb(dbUrl);

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
            resources.addAll(findResource(doc, url));

            logger.info("[lg] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
            service.save(m);
            return m;
        } else {
            findResource(doc, url);
        }

        logger.warn("Cannot find movie for {}: {}", movie.getName(), url);
        service.publishEvent(url, "Cannot find movie for " + movie.getName());
        return null;
    }

    private Set<Resource> findResource(Document doc, String pageUrl) {
        Set<Resource> resources = new HashSet<>();
        if (skipResource) {
            return resources;
        }

        Elements elements = doc.select("div#dwonBT ul li a");
        for (Element element : elements) {
            String uri = element.attr("href");
            if (isResource(uri)) {
                String title = element.text();
                resources.add(service.saveResource(uri, pageUrl, StringUtils.truncate(title, 120)));
            }
        }
        return resources;
    }

    private void getMovie(Document doc, Movie movie) {
        movie.setTitle(doc.select("div.moviedteail_tt h1").text());
        Elements elements = doc.select("div.moviedteail_list li");
        for (Element element : elements) {
            String text = element.text();
            if (text.contains("又名：")) {
                movie.getAliases().addAll(getValues(element));
            } else if (text.contains("类型： ")) {
                movie.setCategories(getCategories(getValues(element)));
            } else if (text.contains("地区： ")) {
                movie.setRegions(getRegions(getValues(element)));
            } else if (text.contains("年份： ")) {
                movie.setYear(service.getYear(text));
            } else if (text.contains("上映： ")) {
                movie.setReleaseDate(getValue(element));
            } else if (text.contains("导演： ")) {
                movie.setDirectors(getPeople(getValues(element)));
            } else if (text.contains("编剧： ")) {
                movie.setEditors(getPeople(getValues(element)));
            } else if (text.contains("主演： ")) {
                movie.setActors(getPeople(getValues(element)));
            } else if (text.contains("imdb： ")) {
                movie.setImdbUrl(UrlUtils.getImdbUrl(text));
            } else if (text.contains("豆瓣： ")) {
                movie.setDbUrl(UrlUtils.getDbUrl(text));
            }
        }

        String html = doc.select("div.mikd .mi_cont div.yp_context").html();
        if (movie.getDbUrl() == null) {
            movie.setDbUrl(UrlUtils.getDbUrl(html));
        }

        if (movie.getImdbUrl() == null) {
            movie.setImdbUrl(UrlUtils.getImdbUrl(html));
        }
    }

    private String getValue(Element element) {
        StringBuilder text = new StringBuilder();
        for (Element a : element.select("a")) {
            text.append(a.text()).append(" ");
        }
        return text.toString();
    }

    private Set<String> getValues(Element element) {
        Set<String> values = new HashSet<>();
        for (Element a : element.select("a")) {
            values.add(a.text());
        }
        return values;
    }

}
