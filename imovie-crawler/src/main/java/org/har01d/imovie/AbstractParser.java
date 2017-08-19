package org.har01d.imovie;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.domain.Category;
import org.har01d.imovie.domain.Language;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Person;
import org.har01d.imovie.domain.Region;
import org.har01d.imovie.douban.DouBanParser;
import org.har01d.imovie.service.MovieService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractParser implements Parser {

    private static final Logger log = LoggerFactory.getLogger(Parser.class);
    private static final Pattern NAME = Pattern.compile("([^ ]+)(第.+季)");

    @Autowired
    protected DouBanParser douBanParser;

    @Autowired
    protected MovieService service;

    protected Movie getByDb(String dbUrl) {
        Movie m = null;
        if (dbUrl != null) {
            m = service.findByDbUrl(dbUrl);
            if (m == null) {
                try {
                    m = douBanParser.parse(dbUrl);
                } catch (IOException e) {
                    log.warn("parse DouBan failed.", e);
                }
            }
        }
        return m;
    }

    protected Movie searchByImdb(Movie movie) {
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

    protected Movie searchByName(Movie movie) {
        if (movie.getName() == null) {
            return null;
        }

        movie.setName(fixName(movie.getName()));
        return searchMovie(movie, movie.getName());
    }

    private String fixName(String name) {
        Matcher matcher = NAME.matcher(name);
        if (matcher.matches()) {
            return matcher.group(1) + " " + matcher.group(2);
        }
        return name;
    }

    protected Movie searchMovie(Movie movie, String text) {
        try {
            List<Movie> movies = douBanParser.search(text);
            return service.findBestMatchedMovie(movies, movie);
        } catch (Exception e) {
            service.publishEvent(text, e.getMessage());
            log.error("search movie from DouBan failed: " + text, e);
        }
        return null;
    }

    protected String getOne(Set<String> set) {
        if (set == null) {
            return null;
        }

        return set.iterator().next();
    }

    protected Set<Category> getCategories(Set<String> names) {
        Set<Category> categories = new HashSet<>();
        for (String name : names) {
            if (name.isEmpty()) {
                continue;
            }
            Category c = new Category(name);
            categories.add(c);
        }
        return categories;
    }

    protected Set<Language> getLanguages(Set<String> names) {
        Set<Language> languages = new HashSet<>();
        for (String name : names) {
            if (name.isEmpty()) {
                continue;
            }
            if ("国语".equals(name) || "普通话".equals(name) || "国语对白".equals(name)) {
                name = "汉语普通话";
            } else if ("国粤".equals(name)) {
                name = "粤语";
            }
            Language l = new Language(name);
            languages.add(l);
        }
        return languages;
    }

    protected Set<Person> getPeople(Set<String> names) {
        Set<Person> people = new HashSet<>();
        for (String name : names) {
            if (name.isEmpty() || "更多…".equals(name) || "内详".equals(name)) {
                continue;
            }
            Person p = new Person(name);
            people.add(p);
        }
        return people;
    }

    protected Set<Region> getRegions(Set<String> names) {
        Set<Region> regions = new HashSet<>();
        for (String name : names) {
            if (name.isEmpty()) {
                continue;
            }
            if ("大陆".equals(name)) {
                name = "中国大陆";
            }
            Region r = new Region(name);
            regions.add(r);
        }
        return regions;
    }

    protected boolean isResource(String uri) {
        return uri != null && (uri.startsWith("magnet") || uri.startsWith("ed2k://") || uri.startsWith("thunder://")
            || uri.startsWith("ftp://") || uri.contains("pan.baidu.com"));
    }

}
