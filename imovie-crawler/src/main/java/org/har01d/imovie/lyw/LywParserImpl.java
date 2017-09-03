package org.har01d.imovie.lyw;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
public class LywParserImpl extends AbstractParser implements LywParser {

    private static final Logger logger = LoggerFactory.getLogger(LywParser.class);

    @Value("${url.lyw.site}")
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
            getMovie(doc, movie);
            m = searchByName(movie);
        }

        if (m != null) {
            m.addResources(findResource(doc, url));
            logger.info("[lyw] get {}/{} resources for movie {}", m.getNewResources(), m.getRes().size(),
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

        Elements elements = doc.select("div.movie div#more-tables table tbody tr");
        logger.info("Resources: {}", elements.size());
        for (Element element : elements) {
            String uri = element.select("td a").first().attr("href");
            if (isResource(uri)) {
                String title = element.text();
                try {
                    if (uri.startsWith("thunder://")) {
                        resources.add(service.saveResource(UrlUtils.convertUrl(uri), uri, title));
                    } else {
                        resources.add(service.saveResource(uri, title));
                    }
                } catch (Exception e) {
                    service.publishEvent(url, e.getMessage());
                    logger.error("[lyw] get resource failed", e);
                }
            } else if (!uri.trim().isEmpty()) {
                logger.warn("{} is not resource", uri);
            }
        }

        return resources;
    }

    private void getMovie(Document doc, Movie movie) throws IOException {
        DateFormat df = new SimpleDateFormat("yyyy-M-d");
        movie.setName(doc.select("div.mySites ul li.active").text());
        try {
            movie.setSourceTime(df.parse(doc.select("div.myDown .myAbbr span").first().ownText().replace("发布时间：", "")));
        } catch (ParseException e) {
            logger.warn("get time failed.", e);
        }

        Element element = doc.select("div.myContent div.col-lg-7").first();
        List<String> lines = convertElement2Lines(element);
        for (String text : lines) {
            getMetadata(movie, text);
        }

        if (!lines.isEmpty()) {
            String text = lines.get(lines.size() - 1);
            if (text.startsWith("http")) {
                movie.setWebsite(text);
            }
        }
    }

    private void getMetadata(Movie movie, String text) {
        if (text.contains("【导演】:")) {
            movie.setDirectors(getPeople(getValues(text, "【导演】:")));
        } else if (text.contains("【主演】:")) {
            movie.setActors(getPeople(getValues(text, "【主演】:")));
        } else if (text.contains("【标签】:")) {
            movie.setCategories(getCategories(getValues(text, "【标签】:")));
        } else if (text.contains("【制片地区/国家】:")) {
            movie.setRegions(getRegions(getValues(text, "【制片地区/国家】:")));
        } else if (text.contains("【语言】:")) {
            movie.setLanguages(getLanguages(getValues(text, "【语言】:")));
        } else if (text.contains("【又名】:")) {
            movie.addAliases(getValues(text, "【又名】:"));
        } else if (text.contains("【原名】:")) {
            movie.addAliases(getValues(text, "【原名】:"));
        } else if (text.contains("【上映时间】:")) {
            movie.setReleaseDate(getValue(text, "【上映时间】:"));
        } else if (text.contains("【年份】:")) {
            movie.setYear(service.getYear(text));
        } else if (text.contains("【片长】:")) {
            movie.setRunningTime(getValue(text, "【片长】:"));
        }/* else if (text.contains("【编辑整理】:")) {
            movie.setWebsite(getValue(text, "【编辑整理】:").replace("[乐游网]www.leyowo.com", ""));
        }*/ else if (text.contains("【IMDb链接】:")) {
            movie.setImdbUrl(UrlUtils.getImdbUrl(getValue(text, "【IMDb链接】:")));
        }
    }

}
