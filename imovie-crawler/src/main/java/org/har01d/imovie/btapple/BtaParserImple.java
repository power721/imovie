package org.har01d.imovie.btapple;

import java.io.IOException;
import java.util.List;
import java.util.Set;
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
public class BtaParserImple implements BtaParser {

    private static final Logger logger = LoggerFactory.getLogger(BtaParser.class);

    @Value("${url.btapple.site}")
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
        String dbUrl = UrlUtils.getDbUrl(html);
        if (dbUrl != null) {
            m = service.findByDbUrl(dbUrl);
            if (m == null) {
                m = douBanParser.parse(dbUrl);
            }
        }

        if (m == null) {
            String imdb = UrlUtils.getImdbUrl(doc.select(".detail .cl a").attr("href"));
            if (imdb != null) {
                m = service.findByImdb(imdb);
                movie.setImdbUrl(imdb);
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
            Elements elements = doc.select(".related table td a");
            for (Element element : elements) {
                String uri = baseUrl + element.attr("href");
                String title = element.text();
                getResource(uri, title, resources);
            }

            logger.info("[BtApple] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
            service.save(m);
            return m;
        }

        logger.warn("Cannot find movie for {}-{}: {}", movie.getName(), movie.getTitle(), url);
        service.publishEvent(url, "Cannot find movie for " + movie.getName() + " - " + movie.getTitle());
        return null;
    }

    private Movie searchByImdb(Movie movie) {
        if (movie.getImdbUrl() == null) {
            return null;
        }

        String imdb = movie.getImdbUrl().replace("http://www.imdb.com/title/", "");
        Movie m = searchMovie(movie, imdb);
        if (m != null) {
            return m;
        }

        return null;
    }

    private Movie searchByName(Movie movie) {
        if (movie.getName() == null) {
            return null;
        }

        return searchMovie(movie, movie.getName());
    }

    private Movie searchMovie(Movie movie, String text) {
        try {
            List<Movie> movies = douBanParser.search(text);
            if (movies.isEmpty()) {
                return null;
            }
            if (movies.size() == 1) {
                return movies.get(0);
            }
//            return findBestMatchedMovie(movies, movie);
        } catch (Exception e) {
            service.publishEvent(text, e.getMessage());
            logger.error("search movie from DouBan failed: " + text, e);
        }
        return null;
    }

    private void getResource(String uri, String title, Set<Resource> resources) {
        try {
            String html = HttpUtils.getHtml(uri);
            Document doc = Jsoup.parse(html);
            String original = null;
            for (Element element : doc.select(".rinfo .tdown a")) {
                String href = element.attr("href");
                if (isResource(href)) {
                    if (original != null) {
                        resources.add(service.saveResource(href, original, title));
                    } else {
                        resources.add(service.saveResource(href, title));
                    }
                } else if (href.startsWith("/d_")) {
                    original = baseUrl + href;
                }
            }
        } catch (IOException e) {
            logger.error("[BtApple] get resource failed: " + uri, e);
            service.publishEvent(uri, "get resource failed: " + uri);
        }
    }

    private boolean isResource(String uri) {
        return uri.startsWith("magnet") || uri.startsWith("ed2k://") || uri.startsWith("thunder://");
    }

}
