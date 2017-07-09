package org.har01d.imovie.zmz;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.AbstractParser;
import org.har01d.imovie.domain.Category;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Person;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.UrlUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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
public class ZmzParserImpl extends AbstractParser implements ZmzParser {

    private static final Logger logger = LoggerFactory.getLogger(ZmzParser.class);
    private Pattern DATE = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");
    private Pattern NAME = Pattern.compile("《(.+)》");

    @Value("${url.zmz.page}")
    private String baseUrl;

    private JSONParser jsonParser = new JSONParser();

    @Override
    @Transactional
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);

        Movie m = null;
        if (movie.getId() == null) {
            getMovie(doc, movie);
            movie.setImdbUrl(UrlUtils.getImdbUrl(html));

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

            if (m == null) {
                String name = getOne(movie.getAliases());
                if (name != null) {
                    m = searchMovie(movie, name);
                }
            }
        } else {
            m = movie;
        }

        String uri = doc.select("div.view-res-list p a").attr("href");
        if (m != null) {
            Set<Resource> resources = m.getRes();
            int size = resources.size();
            resources.addAll(findResource(uri, movie.getName(), url));

            logger.info("[zmz] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
            service.save(m);
            return m;
        } else {
            findResource(uri, movie.getName(), url);
        }

        logger.warn("Cannot find movie for {}: {}", movie.getName(), url);
        service.publishEvent(url, "Cannot find movie for " + movie.getName());
        return null;
    }

    private Set<Resource> findResource(String resourceUri, String name, String pageUrl) {
        Set<Resource> resources = new HashSet<>();
        if (resourceUri.contains("http://xiazai002.com/")) {
            try {
                String html = HttpUtils.getHtml(resourceUri);
                Document doc = Jsoup.parse(html);
                String json = getJson(html);
                if (json == null) {
                    return resources;
                }
                JSONObject jsonObject = (JSONObject) jsonParser.parse(json);
                Elements elements = doc.select(".res-item");

                for (Element dl : elements) {
                    String text = dl.select("dt strong").text();
                    logger.info("findResource: {} {}: {}", name, dl.select("dt").text(),
                        dl.select("dd").size());
                    for (Element element : dl.select("dd")) {
                        String itemid = element.attr("itemid");
                        JSONObject links = (JSONObject) jsonObject.get(itemid);
                        if (links == null) {
                            continue;
                        }

                        String title = text + element.text().replace("查看下载链接", "").trim();
                        if (name != null && !title.contains(name)) {
                            title = name + "-" + title;
                        }
                        title = title.replace("人人下载器专用链下载 ", "");
                        String uri = (String) links.get("1");
                        if (isResource(uri)) {
                            resources.add(service.saveResource(uri, title));
                        }

                        uri = (String) links.get("2");
                        if (isResource(uri)) {
                            resources.add(service.saveResource(uri, title));
                        }

                        uri = (String) links.get("9");
                        if (isResource(uri)) {
                            resources.add(service.saveResource(uri, title));
                        }

                        uri = (String) links.get("102");
                        if (isResource(uri)) {
                            title = title + " 百度云";
                            resources.add(service.saveResource(uri, title));
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("[zmz] get resource failed: " + pageUrl, e);
                service.publishEvent(pageUrl, "get resource failed: " + pageUrl);
            }
        }

        return resources;
    }

    private String getJson(String html) {
        int start = html.indexOf("var file_list=");
        if (start < 0) {
            return null;
        }
        start += "var file_list=".length();
        int end = html.indexOf("}}</script>", start) + 2;
        if (end > start) {
            return html.substring(start, end);
        }
        return null;
    }

    private void getMovie(Document doc, Movie movie) {
        Elements elements = doc.select("div.resource-con .fl-info ul li");
        for (Element element : elements) {
            String text1 = element.select("span").text();
            String text = element.select("strong").text();
            if (text1.contains("原名：")) {
                movie.getAliases().add(text.trim());
            } else if (text1.contains("首播：")) {
                movie.setReleaseDate(text.trim());
            } else if (text1.contains("类型：")) {
                movie.setCategories(getCategories(getValues(text)));
            } else if (text1.contains("地区：")) {
                movie.setRegions(getRegions(getValues(text)));
            } else if (text1.contains("语 言：")) {
                movie.setLanguages(getLanguages(getValues(text)));
            } else if (text1.contains("主演：")) {
                movie.setActors(getPeople(element));
            } else if (text1.contains("編劇：")) {
                movie.setEditors(getPeople(element));
            } else if (text1.contains("导演：")) {
                movie.setDirectors(getPeople(element));
            } else if (text1.contains("别名：")) {
                movie.getAliases().addAll(getValues(text));
            }
        }

        String text = doc.select(".resource-tit h2").text();
        Matcher m = NAME.matcher(text);
        if (m.find()) {
            movie.setName(m.group(1));
        }
        movie.setSynopsis(doc.select("div.resource-desc div.con span").text().trim());
    }

    private Set<String> getValues(String text) {
        Set<String> values = new LinkedHashSet<>();
        String[] vals = text.split("/");
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
            if ("魔幻".equals(name)) {
                name = "奇幻";
            } else if ("纪录".equals(name)) {
                name = "纪录片";
            } else if ("罪案".equals(name)) {
                name = "犯罪";
            } else if ("丧尸".equals(name)) {
                name = "恐怖";
            }
            Category c = new Category(name);
            categories.add(c);
        }
        return categories;
    }

    private Set<Person> getPeople(Element element) {
        Set<Person> people = new HashSet<>();
        for (Element a : element.select("a")) {
            if ("显示全部".equals(a.text())) {
                continue;
            }
            Person p = new Person(a.text());
            people.add(p);
        }
        return people;
    }

    protected Movie searchMovie(Movie movie, String text) {
        try {
            List<Movie> movies = douBanParser.search(text);
            return findBestMatchedMovie(movies, movie);
        } catch (Exception e) {
            service.publishEvent(text, e.getMessage());
            logger.error("[zmz] search movie from DouBan failed: " + text, e);
        }
        return null;
    }

    public Movie findBestMatchedMovie(List<Movie> movies, Movie movie) {
        Movie best = null;
        int maxMatch = 0;
        for (Movie m : movies) {
            int match = 0;
            if (movie.getName().equals(m.getName())) {
                match += 20;
            } else if (m.getName().startsWith(movie.getName())) {
                match += 10;
            }

            if (m.getAliases() != null) {
                for (String name : m.getAliases()) {
                    if (name.equals(movie.getName())) {
                        match += 10;
                        break;
                    }
                }
            }

            if (movie.getCategories() != null && !movie.getCategories().isEmpty() && m.getCategories() != null && !m
                .getCategories().isEmpty()) {
                if (m.getCategories().containsAll(movie.getCategories())) {
                    match += 5;
                } else if (movie.getCategories().containsAll(m.getCategories())) {
                    match += 5;
                }
            }

            if (movie.getRegions() != null && !movie.getRegions().isEmpty() && m.getRegions() != null) {
                if (m.getRegions().containsAll(movie.getRegions())) {
                    match += 5;
                }
            }

            if (movie.getLanguages() != null && !movie.getLanguages().isEmpty() && m.getLanguages() != null) {
                if (m.getLanguages().containsAll(movie.getLanguages())) {
                    match += 5;
                }
            }

            if (movie.getAliases() != null && !movie.getAliases().isEmpty() && m.getAliases() != null) {
                if (m.getAliases().containsAll(movie.getAliases())) {
                    match += 10;
                }
            }

            if (movie.getDirectors() != null && !movie.getDirectors().isEmpty() && m.getDirectors() != null && !m
                .getDirectors().isEmpty()) {
                if (m.getDirectors().containsAll(movie.getDirectors())) {
                    match += 10;
                } else if (movie.getDirectors().containsAll(m.getDirectors())) {
                    match += 10;
                }
            }

            if (movie.getEditors() != null && !movie.getEditors().isEmpty() && m.getEditors() != null && !m.getEditors()
                .isEmpty()) {
                if (m.getEditors().containsAll(movie.getEditors())) {
                    match += 10;
                } else if (movie.getEditors().containsAll(m.getEditors())) {
                    match += 10;
                }
            }

            if (movie.getActors() != null && !movie.getActors().isEmpty() && m.getActors() != null && !m.getActors()
                .isEmpty()) {
                if (m.getActors().containsAll(movie.getActors())) {
                    match += 10;
                } else if (movie.getActors().containsAll(m.getActors())) {
                    match += 10;
                }
            }

            Date date1 = getDates(m.getReleaseDate());
            Date date2 = getDates(movie.getReleaseDate());
            if (date1 != null && date2 != null) {
                if (Math.abs(date1.getTime() - date2.getTime()) <= TimeUnit.DAYS.toMillis(1)) {
                    match += 10;
                }
            }

            if (movie.getImdbUrl() != null && m.getImdbUrl() != null) {
                if (m.getImdbUrl().equals(movie.getImdbUrl())) {
                    match += 15;
                }
            }

            if (movie.getSynopsis() != null && !movie.getSynopsis().isEmpty() && m.getSynopsis() != null) {
                if (m.getSynopsis().equals(movie.getSynopsis())) {
                    match += 20;
                } else if (m.getSynopsis().contains(movie.getSynopsis())) {
                    match += 10;
                }
            }

            if (match > 20 && match > maxMatch) {
                maxMatch = match;
                best = m;
            }
        }

        if (best != null) {
            logger.info("find best matched movie {} for {}, match: {}", best.getName(), movie.getName(), maxMatch);
        }
        return best;
    }

    private Date getDates(String text) {
        if (text == null) {
            return null;
        }

        Matcher m = DATE.matcher(text);
        while (m.find()) {
            String temp = m.group(1) + "-" + m.group(2) + "-" + m.group(3);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            try {
                return df.parse(temp);
            } catch (java.text.ParseException e) {
                logger.warn("get time failed.", e);
            }
        }
        return null;
    }

}
