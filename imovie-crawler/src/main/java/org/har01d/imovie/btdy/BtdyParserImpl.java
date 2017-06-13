package org.har01d.imovie.btdy;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.domain.Category;
import org.har01d.imovie.domain.Language;
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
public class BtdyParserImpl implements BtdyParser {

    private static final Logger logger = LoggerFactory.getLogger(BtdyParser.class);
    private static final Pattern EP = Pattern.compile("(\\d+)集");

    @Value("${url.btdy.site}")
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
        String imdb = movie.getImdbUrl();
        if (imdb != null) {
            m = service.findByImdb(imdb);
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

            logger.info("[btbtdy] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
            service.save(m);
            m.setSourceTime(movie.getSourceTime());
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
        Elements elements = doc.select(".p_list ul li");
        for (Element element : elements) {
            String uri = element.select("span a").attr("href");
            String title = element.select("a").text();
            if (isResource(uri)) {
                resources.add(service.saveResource(uri, title));
            }
        }
        return resources;
    }

    private void getMovie(Document doc, Movie movie) {
        movie.setTitle(doc.select(".vod_intro h1").text());
        movie.setYear(service.getYear(doc.select(".vod_intro h1 span.year").text()));
        if (movie.getName() == null) {
            movie.setName(movie.getTitle());
            if (movie.getYear() != null) {
                movie.setName(movie.getName().replace("(" + movie.getYear() + ")", ""));
            }
        }
        Elements elements = doc.select(".vod_intro dl");
        int phase = 0;
        for (Element element : elements.first().children()) {
            String text = element.text();
            if (element.tagName().equals("dt")) {
                if (text.contains("更新：")) {
                    phase = 1;
                } else if (text.contains("状态：")) {
                    phase = 2;
                } else if (text.contains("类型：")) {
                    phase = 3;
                } else if (text.contains("地区：")) {
                    phase = 4;
                } else if (text.contains("语言：")) {
                    phase = 5;
                } else if (text.contains("imdb：")) {
                    phase = 6;
                } else if (text.contains("主演：")) {
                    phase = 7;
                }
            } else {
                if (phase == 1) {
                    movie.setSourceTime(getSourceTime(text));
                } else if (phase == 2) {
                    movie.setEpisode(getEpisode(text));
                } else if (phase == 3) {
                    movie.setCategories(getCategories(getValues(text.replace("电视剧", "").replace("电影", ""))));
                } else if (phase == 4) {
                    movie.setRegions(getRegions(getValues(text)));
                } else if (phase == 5) {
                    movie.setLanguages(getLanguages(getValues(text)));
                } else if (phase == 6) {
                    movie.setImdbUrl(UrlUtils.getImdbUrl(text));
                } else if (phase == 7) {
                    movie.setActors(getPersons(getValues(text)));
                }
            }
        }

        movie.setSynopsis(doc.select("div.desc div").text().replace("剧情介绍：", "").trim());
    }

    private Date getSourceTime(String text) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            return df.parse(text);
        } catch (ParseException e) {
            logger.warn("get time failed.", e);
        }
        return new Date();
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

    private Set<String> getValues(String text) {
        Set<String> values = new LinkedHashSet<>();
        String[] vals = text.split("[  ]");
        for (String val : vals) {
            val = val.trim();
            if (!val.isEmpty()) {
                values.add(val);
            }
        }

        return values;
    }

    private Set<Category> getCategories(Set<String> names) {
        Set<Category> categories = new HashSet<>();
        for (String name : names) {
            if ("动漫".equals(name)) {
                name = "动画";
            } else if ("纪录".equals(name)) {
                name = "纪录片";
            }
            Category c = new Category(name);
            categories.add(c);
        }
        return categories;
    }

    private Set<Language> getLanguages(Set<String> names) {
        Set<Language> languages = new HashSet<>();
        for (String name : names) {
            if ("国语".equals(name)) {
                name = "汉语普通话";
            }
            Language l = new Language(name);
            languages.add(l);
        }
        return languages;
    }

    private Set<Region> getRegions(Set<String> names) {
        Set<Region> regions = new HashSet<>();
        for (String name : names) {
            if ("大陆".equals(name)) {
                name = "中国大陆";
            }
            Region r = new Region(name);
            regions.add(r);
        }
        return regions;
    }

    private Set<Person> getPersons(Set<String> names) {
        Set<Person> people = new HashSet<>();
        for (String name : names) {
            Person p = new Person(name);
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
            if (movies.isEmpty()) {
                return null;
            }
            if (movies.size() == 1) {
                return movies.get(0);
            }
            return service.findBestMatchedMovie(movies, movie);
        } catch (Exception e) {
            service.publishEvent(text, e.getMessage());
            logger.error("search movie from DouBan failed: " + text, e);
        }
        return null;
    }

    private boolean isResource(String uri) {
        return uri.startsWith("magnet") || uri.startsWith("ed2k://") || uri.startsWith("thunder://");
    }

}
