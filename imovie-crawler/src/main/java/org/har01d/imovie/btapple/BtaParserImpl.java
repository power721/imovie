package org.har01d.imovie.btapple;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.AbstractParser;
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
public class BtaParserImpl extends AbstractParser implements BtaParser {

    private static final Logger logger = LoggerFactory.getLogger(BtaParser.class);
    private static final Pattern EP = Pattern.compile("集数  \\( (\\d+) \\)");

    @Value("${url.btapple.site}")
    private String baseUrl;

    @Override
    @Transactional
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);

        Movie m;
        if (movie.getId() != null) {
            m = service.findById(movie.getId());
        } else {
            getMovie(doc, movie);

            String dbUrl = movie.getDbUrl();
            m = getByDb(dbUrl);

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
        }

        if (m != null) {
            Set<Resource> resources = m.getRes();
            int size = resources.size();
            m.addResources(getResource(doc));

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
        if (skipResource) {
            return resources;
        }

        int zero = 0;
        Elements elements = doc.select(".related table td a");
        for (Element element : elements) {
            String uri = baseUrl + element.attr("href");
            String title = element.text();
            if (service.findResource(uri) == null) {
                if (getResource(uri, title, resources) == 0) {
                    zero++;
                } else {
                    zero = 0;
                }
            }
            if (zero > 10) {
                break;
            }
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

    protected Set<String> getValues(String text) {
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

    private Set<Person> getPeople(Element element) {
        Set<Person> people = new HashSet<>();
        for (Element a : element.select("div a")) {
            Person p = new Person(a.text());
            people.add(p);
        }
        return people;
    }

    private int getResource(String uri, String title, Set<Resource> resources) {
        int count = 0;
        try {
            String html = HttpUtils.getHtml(uri);
            Document doc = Jsoup.parse(html);
            String original = null;
            Resource resource;
            for (Element element : doc.select(".rinfo .tdown a")) {
                String href = element.attr("href");
                if (isResource(href)) {
                    if (original != null) {
                        resource = service.saveResource(href, original, title);
                    } else {
                        resource = service.saveResource(href, uri, title);
                    }
                    if (resource.isNew()) {
                        count++;
                    }
                    resources.add(resource);
                } else if (href.startsWith("/d_")) {
                    original = baseUrl + href;
                }
            }
        } catch (IOException e) {
            logger.error("[BtApple] get resource failed: " + uri, e);
            service.publishEvent(uri, "get resource failed: " + uri);
        }
        return count;
    }

}
