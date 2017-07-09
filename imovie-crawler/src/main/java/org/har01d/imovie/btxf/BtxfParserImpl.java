package org.har01d.imovie.btxf;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.har01d.imovie.AbstractParser;
import org.har01d.imovie.btdy.BtdyParser;
import org.har01d.imovie.domain.Category;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Person;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.util.HttpUtils;
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
public class BtxfParserImpl extends AbstractParser implements BtxfParser {

    private static final Logger logger = LoggerFactory.getLogger(BtdyParser.class);
    private static final String[] TOKENS = new String[]{"类型：", "年份：", "地区：", "语言：", "主演：", "导演：", "评分："};

    @Value("${url.btxf.site}")
    private String siteUrl;

    @Value("${url.btxf.page}")
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
            resources.addAll(getResource(doc));

            logger.info("[btxf] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
            service.save(m);
            m.setSourceTime(movie.getSourceTime());
            return m;
        } else {
//            getResource(doc);
        }

        logger.warn("Cannot find movie for {}-{}: {}", movie.getName(), movie.getTitle(), url);
        service.publishEvent(url, "Cannot find movie for " + movie.getName() + " - " + movie.getTitle());
        return null;
    }

    private Set<Resource> getResource(Document doc) {
        Set<Resource> resources = new HashSet<>();
        Elements elements = doc.select("div#download ul.downloadlist li.d_todo");
        for (Element element : elements) {
            String title = element.previousElementSibling().text();
            String uri = siteUrl + element.select("a").first().attr("href");
            getResource(resources, uri, title);
        }
        return resources;
    }

    private void getResource(Set<Resource> resources, String resUri, String title) {
        try {
            String html = HttpUtils.get(resUri, siteUrl);
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("div.info ul li span a");
            for (Element element : elements) {
                String uri = element.attr("href");
                if (isResource(uri)) {
                    resources.add(service.saveResource(uri, title));
                }
            }
        } catch (IOException e) {
            logger.warn("[btxf] get resource failed", e);
        }
    }

    private void getMovie(Document doc, Movie movie) {
        Elements elements = doc.select("div.info ul li");
        int start;
        int end;
        for (Element element : elements) {
            String text = element.text();
            if (text.contains("地区：")) {
                start = text.indexOf("地区：") + 3;
                end = getNextToken(text, start);
                if (start > 2 && end > start) {
                    movie.setRegions(getRegions(getValues(text.substring(start, end))));
                }
            } else if (text.contains("类型：")) {
                start = text.indexOf("类型：") + 3;
                end = getNextToken(text, start);
                if (start > 2 && end > start) {
                    movie.setCategories(getCategories(getValues(text.substring(start, end))));
                }
            } else if (text.contains("语言：")) {
                start = text.indexOf("语言：") + 3;
                end = getNextToken(text, start);
                if (start > 2 && end > start) {
                    movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
                }
            } else if (text.contains("年份：")) {
                movie.setYear(service.getYear(text));
            } else if (text.contains("评分：")) {
                movie.setImdbUrl(UrlUtils.getImdbUrl(element.html()));
                movie.setDbUrl(UrlUtils.getDbUrl(element.html()));
            } else if (text.contains("导演：")) {
                movie.setDirectors(getPeople(element));
            } else if (text.contains("主演：")) {
                movie.setActors(getPeople(element));
            }
        }
    }

    private int getNextToken(String text, int start) {
        int index = -1;
        for (String token : TOKENS) {
            int i = text.indexOf(token, start);
            if (i > 0 && (i < index || index == -1)) {
                index = i;
            }
        }

        return index;
    }

    private Set<String> getValues(String text) {
        Set<String> values = new LinkedHashSet<>();
        String[] vals = text.split(" ");
        for (String val : vals) {
            val = val.trim();
            if (!val.isEmpty()) {
                values.add(val);
            }
        }

        return values;
    }

    protected Set<Category> getCategories(Set<String> names) {
        Set<Category> categories = new HashSet<>();
        for (String name : names) {
            Category c = new Category(name.replace("电影", ""));
            categories.add(c);
        }
        return categories;
    }

    private Set<Person> getPeople(Element element) {
        Set<Person> people = new HashSet<>();
        for (Element a : element.select("a")) {
            Person p = new Person(a.text());
            people.add(p);
        }
        return people;
    }

}
