package org.har01d.imovie.mp4;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Mp4ParserImpl extends AbstractParser implements Mp4Parser {

    private static final Logger logger = LoggerFactory.getLogger(Mp4Parser.class);
    private static final Pattern EP = Pattern.compile("共(\\d+)集");
    private static final Pattern EP1 = Pattern.compile("第(\\d+)集全");

    @Override
    @Transactional
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);

        getMovie(doc, movie);

        String dbUrl = movie.getDbUrl();
        Movie m = null;
        if (dbUrl != null) {
            m = getByDb(dbUrl);
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
            resources.addAll(findResource(doc, movie.getName()));

            logger.info("[MP4] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
            m.setSourceTime(movie.getSourceTime());
            service.save(m);
            return m;
        } else {
            findResource(doc, movie.getName());
        }

        logger.warn("Cannot find movie for {}: {}", movie.getName(), url);
        service.publishEvent(url, "Cannot find movie for " + movie.getName());
        return null;
    }

    private Set<Resource> findResource(Document doc, String name) {
        Set<Resource> resources = new HashSet<>();
        Elements elements = doc.select("ul#ul1 li a");
        for (Element element : elements) {
            String uri = element.attr("href");
            if (isResource(uri)) {
                String title = element.text();
                if (!title.contains(name)) {
                    title = name + "-" + title;
                }
                try {
                    resources
                        .add(service.saveResource(UrlUtils.convertUrl(uri), uri, StringUtils.truncate(title, 120)));
                } catch (Exception e) {
                    service.publishEvent(name, e.getMessage());
                    logger.error("[mp4] get resource failed", e);
                }
            }
        }
        return resources;
    }

    private void getMovie(Document doc, Movie movie) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        movie.setTitle(doc.select("div.info h1").text().replace("电影下载", "").replace("电视剧下载", "").replace("国语", ""));
        if (movie.getName() == null) {
            movie.setName(movie.getTitle());
        }
        String type = "";

        Elements elements = doc.select("div.info div");
        for (Element element : elements) {
            String text = element.text();
            if (text.contains("语言：")) {
                int index = text.indexOf("语言：") + "语言：".length();
                String temp = text.substring(index, text.length());
                if ("国语".equals(temp)) {
                    temp = "汉语普通话";
                }
                if (!"未知".equals(temp)) {
                    movie.setLanguages(getLanguages(new HashSet<>(Arrays.asList(temp.split(" / ")))));
                }
            }
            if (text.contains("剧情：")) {
                movie.setCategories(getCategories(getValues(element)));
            }
            if (text.contains("地区：")) {
                String temp = element.select("label").first().text();
                int index = temp.indexOf("地区：") + "地区：".length();
                temp = temp.substring(index, temp.length());
                if ("其它".equals(temp)) {
                    if ("大陆剧".equals(type)) {
                        temp = "中国大陆";
                    }
                }
                if ("大陆".equals(temp)) {
                    temp = "中国大陆";
                }
                movie.setRegions(getRegions(new HashSet<>(Arrays.asList(temp.split(" , ")))));
            }
            if (text.contains("分类：")) {
                int index = text.indexOf("分类：") + "分类：".length();
                type = text.substring(index, text.length());
            }
            if (text.contains("年代：")) {
                movie.setYear(service.getYear(text));
            }
            if (text.contains("状态：")) {
                movie.setEpisode(getEpisode(text));
            }
            if (text.contains("导演：")) {
                movie.setDirectors(getPeople(getValues2(element)));
//                int index = text.indexOf("导演：") + "导演：".length();
//                movie.setDirectors(getPeople(Collections.singleton(text.substring(index, text.length()))));
            }
            if (text.contains("主演：")) {
                movie.setActors(getPeople(getValues2(element)));
            }
            if (text.contains("更新时间：")) {
                text = element.select("label").first().text();
                int index = text.indexOf("更新时间：") + "更新时间：".length();
                try {
                    movie.setSourceTime(df.parse(text.substring(index, text.length())));
                } catch (ParseException e) {
                    logger.error("[mp4] Parse date failed.", e);
                }
            }
        }

        String html = doc.select("ul.description").html();
        if (movie.getDbUrl() == null) {
            movie.setDbUrl(UrlUtils.getDbUrl(html));
        }

        if (movie.getImdbUrl() == null) {
            movie.setImdbUrl(UrlUtils.getImdbUrl(html));
        }
    }

    private Integer getEpisode(String text) {
        Matcher matcher = EP.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.valueOf(matcher.group(1));
            } catch (NumberFormatException e) {
                // ignore
            }
            return 0;
        } else {
            matcher = EP1.matcher(text);
            if (matcher.find()) {
                try {
                    return Integer.valueOf(matcher.group(1));
                } catch (NumberFormatException e) {
                    // ignore
                }
                return 0;
            }
        }
        return null;
    }

    private Set<String> getValues(Element element) {
        Set<String> values = new HashSet<>();
        for (Element a : element.select("a")) {
            String name = a.text().trim();
            if (!name.isEmpty()) {
                if ("cult".equals(name) || "SM".equals(name)) {
                    continue;
                }
                if ("言情".equals(name)) {
                    name = "爱情";
                }
                values.add(name);
            }
        }
        return values;
    }

    private Set<String> getValues2(Element element) {
        Set<String> values = new HashSet<>();
        for (Element a : element.select("a")) {
            if (!a.text().isEmpty()) {
                values.add(a.text().replace(' ', '·').trim());
            }
        }
        return values;
    }

}
