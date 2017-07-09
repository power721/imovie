package org.har01d.imovie.fix;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.AbstractParser;
import org.har01d.imovie.domain.Category;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Person;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.StringUtils;
import org.har01d.imovie.util.UrlUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FixParserImpl extends AbstractParser implements FixParser {

    private static final Logger logger = LoggerFactory.getLogger(FixParser.class);
    private Pattern NUMBER = Pattern.compile("(\\d+)");
    private static final String[] TOKENS = new String[]{"导演:", "编剧:", "主演:", "类型:", "制片国家/地区:", "语言:",
        "上映日期:", "日期:", "上映时间:", "片长:", "又名:", "IMDb链接:", "官方网站:", "首播:", "季数:", "集数:", "单集片长:"
        , "【剧集简介】", "【剧情简介】", "【资源下载】"};

    @Value("${url.fix}")
    private String baseUrl;

    @Override
    @Transactional
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
            resources.addAll(findResource(doc, movie.getName(), url));

            logger.info("[fix] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
            service.save(m);
            return m;
        } else {
            findResource(doc, movie.getName(), url);
        }

        logger.warn("Cannot find movie for {}: {}", movie.getName(), url);
        service.publishEvent(url, "Cannot find movie for " + movie.getName());
        return null;
    }

    private Set<Resource> findResource(Document doc, String name, String pageUrl) {
        Set<Resource> resources = new HashSet<>();
        Elements elements = doc.select("div.content-box a");
        for (Element element : elements) {
            String uri = element.attr("href");
            if (isResource(uri)) {
                String title = name + " " + element.text();
                Node node = element.parent().childNode(0);
                if (node instanceof TextNode) {
                    title = name + "-" + ((TextNode) node).text() + element.text();
                }

                resources.add(service.saveResource(uri, pageUrl, StringUtils.truncate(title, 120)));
            }
        }
        return resources;
    }

    private void getMovie(Document doc, Movie movie) {
        String text = doc.select("div.content-box").text();
        int start = text.indexOf("制片国家/地区:") + 8;
        int end = getNextToken(text, start);
        if (start > 7 && end > start) {
            movie.setRegions(getRegions(getValues(text.substring(start, end))));
        }

        start = text.indexOf("类型:") + 3;
        end = getNextToken(text, start);
        if (start > 2 && end > start) {
            movie.setCategories(getCategories(getValues(text.substring(start, end))));
        }

        start = text.indexOf("语言:") + 3;
        end = getNextToken(text, start);
        if (start > 2 && end > start) {
            movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
        }

        start = text.indexOf("导演:") + 3;
        end = getNextToken(text, start);
        if (start > 2 && end > start) {
            movie.setDirectors(getPeople(getValues(text.substring(start, end))));
        }

        start = text.indexOf("编剧:") + 3;
        end = getNextToken(text, start);
        if (start > 2 && end > start) {
            movie.setEditors(getPeople(getValues(text.substring(start, end))));
        }

        start = text.indexOf("主演:") + 3;
        end = getNextToken(text, start);
        if (start > 2 && end > start) {
            movie.setActors(getPeople(getValues(text.substring(start, end))));
        }

        start = text.indexOf("上映日期:") + 5;
        end = getNextToken(text, start);
        if (start > 4 && end > start) {
            movie.setReleaseDate(getValue(text.substring(start, end), 120));
        }

        start = text.indexOf("首播:") + 3;
        end = getNextToken(text, start);
        if (start > 2 && end > start) {
            movie.setReleaseDate(getValue(text.substring(start, end), 120));
        }

        start = text.indexOf("片长:") + 3;
        end = getNextToken(text, start);
        if (start > 2 && end > start) {
            movie.setRunningTime(getValue(text.substring(start, end), 120));
        }

        start = text.indexOf("单集片长:") + 5;
        end = getNextToken(text, start);
        if (start > 4 && end > start) {
            movie.setRunningTime(getValue(text.substring(start, end), 120));
        }

        start = text.indexOf("IMDb") + 4;
        end = getNextToken(text, start);
        if (start > 3 && end > start) {
            movie.setImdbUrl(UrlUtils.getImdbUrl(text.substring(start, end)));
        }

        start = text.indexOf("集数:") + 3;
        end = getNextToken(text, start);
        if (start > 2 && end > start) {
            movie.setEpisode(getNumber(text.substring(start, end)));
        }

        start = text.indexOf("又名:") + 3;
        end = getNextToken(text, start);
        if (start > 2 && end > start) {
            movie.getAliases().addAll(getValues(text.substring(start, end)));
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

    private int getNumber(String text) {
        Matcher matcher = NUMBER.matcher(text);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }
        return 0;
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

    private Set<Category> getCategories(Set<String> names) {
        Set<Category> categories = new HashSet<>();
        for (String name : names) {
            Category c = new Category(name);
            categories.add(c);
        }
        return categories;
    }

    private Set<Person> getPeople(Set<String> names) {
        Set<Person> people = new HashSet<>();
        for (String name : names) {
            if ("更多…".equals(name)) {
                continue;
            }
            Person p = new Person(name);
            people.add(p);
        }
        return people;
    }

}
