package org.har01d.imovie.mjtt;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
public class MjttParserImpl extends AbstractParser implements MjttParser {

    private static final Logger logger = LoggerFactory.getLogger(MjttParser.class);
    private static final Pattern EP = Pattern.compile("共(\\d+)集");

    @Value("${url.mjtt.site}")
    private String siteUrl;

    @Override
    @Transactional
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url, "GBK");
        Document doc = Jsoup.parse(html);

        Movie m;
        if (movie.getId() != null) {
            m = service.findById(movie.getId());
        } else {
            getMovie(doc, movie);
            m = searchByName(movie);
        }

        if (m != null) {
            m.addResources(findResource(doc, movie.getName()));
            logger.info("[mjtt] get {}/{} resources for movie {}", m.getNewResources(), m.getRes().size(),
                m.getName());
            service.save(m);
            m.setCompleted(movie.isCompleted());
            m.setSourceTime(movie.getSourceTime());
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
        if (skipResource) {
            return resources;
        }

        Elements elements = doc.select("div.down_list ul li p a");
        if (elements.size() >= 50) {
            logger.info("Resources: {}", elements.size());
        }
        for (Element element : elements) {
            String uri = element.attr("href");
            if (isResource(uri)) {
                String title = element.text();
                try {
                    if (uri.startsWith("thunder://")) {
                        resources.add(service.saveResource(UrlUtils.convertUrl(uri), uri, title));
                    } else {
                        resources.add(service.saveResource(uri, title));
                    }
                } catch (Exception e) {
                    service.publishEvent(name, e.getMessage());
                    logger.error("[mjtt] get resource failed", e);
                }
            } else if (!uri.trim().isEmpty()) {
                logger.warn("{} is not resource", uri);
            }
        }

        elements = doc.select("div.wp-list ul li");
        if (elements.size() >= 50) {
            logger.info("Resources: {}", elements.size());
        }
        for (Element element : elements) {
            String uri = element.select("a").attr("href");
            if (isResource(uri)) {
                String title = element.text().replace("点击进入", "");
                try {
                    resources.add(service.saveResource(uri, title));
                } catch (Exception e) {
                    service.publishEvent(name, e.getMessage());
                    logger.error("[mjtt] get resource failed", e);
                }
            } else if (!uri.trim().isEmpty()) {
                logger.warn("{} is not resource", uri);
            }
        }

        return resources;
    }

    private void getMovie(Document doc, Movie movie) throws IOException {
        DateFormat df = new SimpleDateFormat("yyyy/M/d HH:mm:ss");
        movie.setTitle(doc.select("div.info-title").text());
        movie.setName(doc.select("div.info-title h1").text());
        movie.setYear(service.getYear(doc.select("div.info-title").first().ownText()));

        Elements elements = doc.select("div.info-box ul li");
        movie.setEpisode(getEpisode(elements.first().text()));
        for (Element element : elements) {
            String text = element.text();
            if (text.contains("原名：")) {
                movie.addAliases(getValues(element.ownText()));
            } else if (text.contains("别名：")) {
                movie.addAliases(getValues(element.ownText()));
            } else if (text.contains("首播日期：")) {
                movie.setReleaseDate(element.ownText());
            } else if (text.contains("单集片长：")) {
                movie.setRunningTime(element.select("label").last().ownText());
            } else if (text.contains("地区：")) {
                movie.setRegions(getRegions(getValues(element.select("label").first().ownText())));
            } else if (text.contains("剧情：")) {
                movie.setCategories(getCategories(getValues2(element.ownText())));
            } else if (text.contains("导演：")) {
                movie.setDirectors(getPeople(getValues(element)));
            } else if (text.contains("编剧：")) {
                movie.setEditors(getPeople(getValues(element)));
            } else if (text.contains("主演：")) {
                movie.setActors(getPeople(getValues(element)));
            } else if (text.contains("时间：")) {
                try {
                    movie.setSourceTime(df.parse(element.select("label").first().ownText()));
                } catch (ParseException e) {
                    logger.warn("get time failed.", e);
                }
            } else if (text.contains("本季终") || text.contains("全剧完结")) {
                movie.setCompleted(true);
            }
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
        }
        return null;
    }

    protected Set<String> getValues(Element element) {
        Set<String> values = new HashSet<>();
        for (Element a : element.select("a")) {
            String name = a.text().trim();
            if (!name.isEmpty()) {
                values.add(name);
            }
        }
        return values;
    }

    protected Set<String> getValues(String text) {
        Set<String> values = new HashSet<>();
        String[] temp = text.split("/");
        for (String name : temp) {
            name = name.trim();
            if (!name.isEmpty()) {
                values.add(name);
            }
        }
        return values;
    }

    private Set<String> getValues2(String text) {
        Set<String> values = new HashSet<>();
        String[] temp = text.split(",");
        for (String name : temp) {
            name = name.trim();
            if (!name.isEmpty()) {
                values.add(name);
            }
        }
        return values;
    }

}
