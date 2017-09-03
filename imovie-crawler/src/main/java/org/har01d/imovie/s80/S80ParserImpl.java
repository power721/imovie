package org.har01d.imovie.s80;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.client.HttpResponseException;
import org.har01d.imovie.AbstractParser;
import org.har01d.imovie.domain.Movie;
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
public class S80ParserImpl extends AbstractParser implements S80Parser {

    private static final Logger logger = LoggerFactory.getLogger(S80Parser.class);
    private static final Pattern EP = Pattern.compile("共(\\d+)集");
    private static final Pattern EP1 = Pattern.compile("第(\\d+)集(全)");

    @Value("${url.s80.site}")
    private String siteUrl;

    @Override
    @Transactional
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);

        Movie m;
        if (movie.getId() != null) {
            m = service.findById(movie.getId());
        } else {
            String dbUrl = UrlUtils.getDbUrl(doc.select("div#minfo .info div span").html());
            m = getByDb(dbUrl);
            if (m == null) {
                getMovie(doc, movie);
                m = searchByName(movie);
            }
        }

        if (m != null) {
            m.addResources(findResource(doc, url));
            logger.info("[80s] get {}/{} resources for movie {}", m.getNewResources(), m.getRes().size(),
                m.getName());
            service.save(m);
            m.setCompleted(movie.isCompleted());
            m.setSourceTime(movie.getSourceTime());
            return m;
        } else {
            findResource(doc, url);
        }

        logger.warn("Cannot find movie for {}: {}", movie.getName(), url);
        service.publishEvent(url, "Cannot find movie for " + movie.getName());
        return null;
    }

    private Set<Resource> findResource(Document doc, String url) {
        Set<Resource> resources = new HashSet<>();
        if (skipResource) {
            return resources;
        }

        resources.addAll(getResource(doc, url));
        Elements elements = doc.select("ul#cpage li");
        int i = 0;
        for (Element element : elements) {
            if (i++ == 0) {
                continue;
            }
            String text = element.select("span").last().text();
            if (text.contains("电视格式下载")) {
                resources.addAll(findResource(url + "/bt-1"));
            } else if (text.contains("平板MP4下载")) {
                resources.addAll(findResource(url + "/bd-1"));
            } else if (text.contains("手机MP4下载")) {
                resources.addAll(findResource(url + "/hd-1"));
            } else if (text.contains("小MP4下载")) {
                resources.addAll(findResource(url + "/mp4-1"));
            }
        }
        return resources;
    }

    private Set<Resource> findResource(String url) {
        try {
            String html = HttpUtils.getHtml(url);
            Document doc = Jsoup.parse(html);
            return getResource(doc, url);
        } catch (Exception e) {
            if (e instanceof HttpResponseException && ((HttpResponseException) e).getStatusCode() == 404) {
                return Collections.emptySet();
            }
            service.publishEvent(url, e.getMessage());
            logger.error("[80s] get resource failed", e);
        }

        return Collections.emptySet();
    }

    private Set<Resource> getResource(Document doc, String url) {
        Set<Resource> resources = new HashSet<>();
        Elements elements = doc.select("ul.dllist1 li span.dlname span a");
        if (elements.size() >= 50) {
            logger.info("Resources: {}", elements.size());
        }
        for (Element element : elements) {
            String uri = element.attr("href");
            if (isResource(uri)) {
                String title = element.text().trim() + " " + element.parent().ownText().trim();
                try {
                    if (uri.startsWith("thunder://")) {
                        resources.add(service.saveResource(UrlUtils.convertUrl(uri), uri, title));
                    } else {
                        resources.add(service.saveResource(uri, title));
                    }
                } catch (Exception e) {
                    service.publishEvent(url, e.getMessage());
                    logger.error("[80s] get resource failed", e);
                }
            } else if (!uri.trim().isEmpty()) {
                logger.warn("{} is not resource", uri);
            }
        }
        return resources;
    }

    private void getMovie(Document doc, Movie movie) throws IOException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        movie.setName(doc.select("div#minfo .info h1").text().replace("未删减版", ""));
        movie.setYear(service.getYear(doc.select("div#minfo .info").first().ownText()));

        movie.setSynopsis(doc.select("meta[property=og:description]").attr("content"));

        Elements elements = doc.select("div#minfo .info span.font_888");
        for (Element element : elements) {
            String text = element.text();
            if (text.contains("又名：")) {
                movie.addAliases(getValues(element.parent().ownText()));
            } else if (text.contains("演员：")) {
                movie.setActors(getPeople(getValues(element.parent())));
            } else if (text.contains("导演：")) {
                movie.setDirectors(getPeople(getValues(element.parent())));
            } else if (text.contains("类型：")) {
                movie.setCategories(getCategories(getValues(element.parent())));
            } else if (text.contains("地区：")) {
                movie.setRegions(getRegions(getValues(element.parent().ownText())));
            } else if (text.contains("上映日期：")) {
                movie.setReleaseDate(element.parent().ownText());
            } else if (text.contains("片长：")) {
                movie.setRunningTime(element.parent().ownText());
            } else if (text.contains("更新日期：")) {
                text = element.parent().ownText();
                try {
                    movie.setSourceTime(df.parse(text));
                } catch (ParseException e) {
                    logger.warn("get time failed.", e);
                }
            }
        }

        String text = doc.select("div#minfo .info span").first().text();
        if (text.contains("(全)")) {
            movie.setCompleted(true);
        }
        movie.setEpisode(getEpisode(text));
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
        }

        matcher = EP1.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.valueOf(matcher.group(1));
            } catch (NumberFormatException e) {
                // ignore
            }
            return 0;
        }
        return null;
    }

    protected Set<String> getValues(String text) {
        Set<String> values = new HashSet<>();
        for (String name : text.split(",")) {
            name = name.replace(" ", "").trim();
            if ("大陆".equals(name)) {
                name = "中国大陆";
            }
            if (!name.isEmpty()) {
                values.add(name);
            }
        }
        return values;
    }

}
