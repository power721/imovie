package org.har01d.imovie.btdy;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.AbstractParser;
import org.har01d.imovie.domain.Category;
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
public class BtdyParserImpl extends AbstractParser implements BtdyParser {

    private static final Logger logger = LoggerFactory.getLogger(BtdyParser.class);
    private static final Pattern EP = Pattern.compile("(\\d+)集");

    @Value("${url.btdy.site}")
    private String baseUrl;

    @Override
    @Transactional
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);

        Movie m = null;
        if (movie.getId() != null) {
            m = service.findById(movie.getId());
        } else {
            getMovie(doc, movie);

            String imdb = movie.getImdbUrl();
            if (imdb != null) {
                m = service.findByImdb(imdb);
            }

//            if (m == null) {
//                m = searchByImdb(movie);
//            }

            if (m == null) {
                m = searchByName(movie);
            }
        }

        if (m != null) {
            Set<Resource> resources = m.getRes();
            int size = resources.size();
            resources.addAll(getResource(doc));

            logger.info("[btbtdy] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
            service.save(m);
            m.setNewResources(resources.size() - size);
            m.setCompleted(movie.isCompleted());
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
                if (text.contains("更新:")) {
                    phase = 1;
                } else if (text.contains("状态:")) {
                    phase = 2;
                } else if (text.contains("类型:")) {
                    phase = 3;
                } else if (text.contains("地区:")) {
                    phase = 4;
                } else if (text.contains("语言:")) {
                    phase = 5;
                } else if (text.contains("imdb:")) {
                    phase = 6;
                } else if (text.contains("主演:")) {
                    phase = 7;
                }
            } else {
                if (phase == 1) {
                    movie.setSourceTime(getSourceTime(text));
                } else if (phase == 2) {
                    movie.setEpisode(getEpisode(text));
                    if (text.contains("完结")) {
                        movie.setCompleted(true);
                    }
                } else if (phase == 3) {
                    movie.setCategories(getCategories(getValues(text.replace("电视剧", "").replace("电影", ""))));
                } else if (phase == 4) {
                    movie.setRegions(getRegions(getValues(text)));
                } else if (phase == 5) {
                    movie.setLanguages(getLanguages(getValues(text)));
                } else if (phase == 6) {
                    movie.setImdbUrl(UrlUtils.getImdbUrl(text));
                } else if (phase == 7) {
                    movie.setActors(getPeople(getValues(text)));
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

    protected Set<Category> getCategories(Set<String> names) {
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

}
