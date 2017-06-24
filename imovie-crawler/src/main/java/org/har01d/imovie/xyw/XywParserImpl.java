package org.har01d.imovie.xyw;

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
import org.har01d.imovie.util.StringUtils;
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
import org.springframework.transaction.annotation.Transactional;

@Service
public class XywParserImpl implements XywParser {

    private static final Logger logger = LoggerFactory.getLogger(XywParserImpl.class);
    private static final Pattern EP = Pattern.compile("共有(\\d+)集");

    @Value("${url.xyw}")
    private String baseUrl;

    @Autowired
    private DouBanParser douBanParser;

    @Autowired
    private MovieService service;

    @Override
    @Transactional
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

        String resUri = url.replace(".html", "").replace("/tv/", "/videos/resList/").replace("/movie/", "/videos/resList/");
        if (m != null) {
            Set<Resource> resources = m.getRes();
            int size = resources.size();
            resources.addAll(getResource(resUri, null));

            logger.info("[xyw] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
            service.save(m);
            return m;
        } else {
            getResource(resUri, movie.getName());
        }

        logger.warn("Cannot find movie for {}-{}: {}", movie.getName(), movie.getTitle(), url);
        service.publishEvent(url, "Cannot find movie for " + movie.getName() + " - " + movie.getTitle());
        return null;
    }

    private Set<Resource> getResource(String resUri, String name) {
        Set<Resource> resources = new HashSet<>();
        try {
            String html = HttpUtils.getHtml(resUri);
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select(".tab-content a");
            for (Element element : elements) {
                String uri = element.attr("href");
                if (isResource(uri)) {
                    String title = element.text();
                    if (uri.contains("pan.baidu.com")) {
                        title = element.parent().text();
                    }
                    if (name != null && !title.contains(name)) {
                        title = name + "-" +title;
                    }
                    resources.add(service.saveResource(uri, title));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resources;
    }

    private void getMovie(Document doc, Movie movie) {
        Elements elements = doc.select(".movie-info table.table-striped tr");
        for (Element element : elements) {
            String text = element.text();
            if (text.contains("又名")) {
                movie.setAliases(getValues(text.replace("又名", "").trim()));
            } else if (text.contains("地区")) {
                movie.setRegions(getRegions(getValues(text.replace("地区", "").trim())));
            } else if (text.contains("类型")) {
                movie.setCategories(getCategories(getValues(text.replace("类型", "").trim())));
            } else if (text.contains("语言")) {
                movie.setLanguages(getLanguages(getValues(text.replace("语言", "").trim())));
            } else if (text.contains("上映时间")) {
                movie.setReleaseDate(getValue(text.replace("上映时间", "").trim(), 120));
            } else if (text.contains("片长")) {
                movie.setRunningTime(getValue(text.replace("片长", "").trim(), 120));
            } else if (text.contains("评分")) {
                movie.setImdbUrl(UrlUtils.getImdbUrl(element.html()));
                movie.setDbUrl(UrlUtils.getDbUrl(element.html()));
            } else if (text.contains("导演")) {
                movie.setDirectors(getPeople(element));
            } else if (text.contains("编剧")) {
                movie.setDirectors(getPeople(element));
            } else if (text.contains("主演")) {
                movie.setActors(getPeople(element));
            } else if (text.contains("其他")) {
                movie.setEpisode(getEpisode(text));
            }
        }
        String text = doc.select(".movie-info h1").text();
        movie.setYear(service.getYear(text));
        movie.setTitle(text);
        movie.setSourceTime(getSourceTime(doc.select(".movie-info a.movie-post").first().nextElementSibling().text()));
    }

    private String getValue(String text, int len) {
        text = text.replaceAll("　", "").replaceAll(" ", "").replaceAll("：", "").replaceAll(" ", "").trim();
        return StringUtils.truncate(text, len);
    }

    private Set<String> getValues(String text) {
        Set<String> values = new LinkedHashSet<>();
        String[] vals = text.split(" / ");
        for (String val : vals) {
            val = val.trim();
            if (!val.isEmpty()) {
                values.add(val);
            }
        }

        return values;
    }

    private Date getSourceTime(String text) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return df.parse(text.replace("最后更新：", ""));
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

    private Set<Category> getCategories(Set<String> names) {
        Set<Category> categories = new HashSet<>();
        for (String name : names) {
            Category c = new Category(name);
            categories.add(c);
        }
        return categories;
    }

    private Set<Language> getLanguages(Set<String> names) {
        Set<Language> languages = new HashSet<>();
        for (String name : names) {
            if ("国语".equals(name) || "普通话".equals(name)) {
                name = "汉语普通话";
            } else if ("国粤".equals(name)) {
                name = "粤语";
            }
            Language l = new Language(name);
            languages.add(l);
        }
        return languages;
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
        for (Element a : element.select("td a")) {
            if ("显示全部".equals(a.text())) {
                continue;
            }
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

    private boolean isResource(String uri) {
        return uri.startsWith("magnet") || uri.startsWith("ed2k://") || uri.startsWith("thunder://")
            || uri.contains("pan.baidu.com");
    }

}
