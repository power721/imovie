package org.har01d.imovie.btapple;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.domain.Category;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Person;
import org.har01d.imovie.domain.Region;
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
    private static final Pattern EP = Pattern.compile("集数  \\( (\\d+) \\)");

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

        getMovie(doc, movie);

        Movie m = null;
        String dbUrl = movie.getDbUrl();
        if (dbUrl != null) {
            m = service.findByDbUrl(dbUrl);
            if (m == null) {
                m = douBanParser.parse(dbUrl);
            }
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
            resources.addAll(getResource(doc));

            logger.info("[BtApple] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
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
        Elements elements = doc.select(".related table td a");
        for (Element element : elements) {
            String uri = baseUrl + element.attr("href");
            String title = element.text();
            getResource(uri, title, resources);
        }
        return resources;
    }

    private void getMovie(Document doc, Movie movie) {
        Elements elements = doc.select("ul.detail li");
        for (Element element : elements) {
            String text = element.text();
            if (text.contains("又名: ")) {
                movie.setAliases(getValues(text.replace("又名: ", "").trim()));
            } else if (text.contains("地区: ")) {
                movie.setRegions(getRegions(getValues(text.replace("地区: ", "").trim())));
            } else if (text.contains("类型: ")) {
                movie.setCategories(getCategories(getValues(text.replace("类型: ", "").trim())));
            } else if (text.contains("imdb: ")) {
                movie.setImdbUrl(UrlUtils.getImdbUrl(text));
            } else if (text.contains("豆 瓣: ")) {
                movie.setDbUrl(UrlUtils.getDbUrl(element.select("div a").attr("href")));
            } else if (text.contains("导演: ")) {
                movie.setDirectors(getPeople(element));
            } else if (text.contains("编剧: ")) {
                movie.setDirectors(getPeople(element));
            } else if (text.contains("主演: ")) {
                movie.setActors(getPeople(element));
            }
        }
        String text = doc.select(".rtitle h1").text();
        movie.setYear(service.getYear(text));
        movie.setEpisode(getEpisode(text));
    }

    private Set<String> getValues(String text) {
        Set<String> values = new LinkedHashSet<>();
        String[] vals = text.split(",");
        for (String val : vals) {
            val = val.trim();
            if (!val.isEmpty()) {
                values.add(val);
            }
        }

        return values;
    }

    private int getEpisode(String text) {
        Matcher matcher = EP.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.valueOf(matcher.group(1));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return 0;
    }

    private Set<Category> getCategories(Set<String> names) {
        Set<Category> categories = new HashSet<>();
        for (String name : names) {
            Category c = new Category(name);
            categories.add(c);
        }
        return categories;
    }

    private Set<Region> getRegions(Set<String> names) {
        Set<Region> regions = new HashSet<>();
        for (String name : names) {
            Region r = new Region(name);
            regions.add(r);
        }
        return regions;
    }

    private Set<Person> getPeople(Element element) {
        Set<Person> people = new HashSet<>();
        for (Element a : element.select("div a")) {
            Person p = new Person(a.text());
            people.add(p);
        }
        return people;
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
            return service.findBestMatchedMovie(movies, movie);
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
