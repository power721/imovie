package org.har01d.imovie.ck;

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
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Person;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.util.HttpUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CkParserImpl extends AbstractParser implements CkParser {

    private static final Logger logger = LoggerFactory.getLogger(CkParser.class);
    private static final Pattern EP = Pattern.compile("共(\\d+)集");

    private JSONParser jsonParser = new JSONParser();

    @Override
    @Transactional
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);

        getMovie(doc, movie);

        Movie m = searchByName(movie);

        if (m != null) {
            Set<Resource> resources = m.getRes();
            int size = resources.size();
            resources.addAll(findResource(doc));

            logger.info("[ck] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
            service.save(m);
            return m;
        } else {
            findResource(doc);
        }

        logger.warn("Cannot find movie for {}: {}", movie.getName(), url);
        service.publishEvent(url, "Cannot find movie for " + movie.getName());
        return null;
    }

    private Set<Resource> findResource(Document doc) {
        Set<Resource> resources = new HashSet<>();
        String id = doc.select("input#comment_post_ID").val();
        String uri = "http://api.hzxdr.cn/api/json_" + id + ".json";
        findResource(uri, resources);
        return resources;
    }

    private void findResource(String uri, Set<Resource> resources) {
        try {
            String json = HttpUtils.getJson(uri);
            JSONArray items = (JSONArray) jsonParser.parse(json.substring(1, json.length() - 1));
            if (items == null || items.isEmpty()) {
                return;
            }

            for (Object item1 : items) {
                JSONObject item = (JSONObject) item1;
                String ed2k = (String) item.get("ed2k");
                if (isResource(ed2k)) {
                    String title = item.get("fileNameed2k") + " " + item.get("size_ed2k");
                    resources.add(service.saveResource(ed2k, title));
                }

                String magnet = (String) item.get("magnet");
                if (isResource(magnet)) {
                    String title = item.get("filename") + " " + item.get("size");
                    resources.add(service.saveResource(magnet, title));
                }
            }
        } catch (Exception e) {
            logger.warn("[ck] get resource failed", e);
        }
    }

    private void getMovie(Document doc, Movie movie) {
        movie.setSynopsis(doc.select("span#short_summary_all").text().replace("[收起]", ""));
        Elements elements = doc.select("div#video-list span");
        movie.setEpisode(getEpisode(doc.select("div#video-list").text()));
        int phase = 0;
        for (Element element : elements) {
            String text = element.text();
            if (text.contains("又名：")) {
                phase = 1;
            } else if (text.contains("类型：")) {
                phase = 4;
            } else if (text.contains("地区：")) {
                phase = 6;
            } else if (text.contains("年代：")) {
                phase = 5;
            } else if (text.contains("导演：")) {
                phase = 3;
                Element next = element.nextElementSibling();
                Set<Person> people = new HashSet<>();
                while (next != null && next.tagName().equals("a")) {
                    Person p = new Person(next.text());
                    people.add(p);
                    next = next.nextElementSibling();
                }
                movie.setDirectors(people);
            } else if (text.contains("主演：")) {
                phase = 2;
            } else if (text.contains("更新：")) {
                phase = 7;
            } else if (text.contains("更新至：")) {
                phase = 0;
            } else if (text.contains("简介：")) {
                phase = 0;
            } else {
                if (phase == 1) {
                    movie.getAliases().addAll(getValues(text));
                } else if (phase == 2) {
                    movie.setActors(getPeople(getValues(element)));
                } else if (phase == 3) {
                    movie.setDirectors(getPeople(getValues(element)));
                } else if (phase == 4) {
                    movie.setCategories(getCategories(getValues(element)));
                } else if (phase == 5) {
                    movie.setYear(service.getYear(text));
                } else if (phase == 6) {
                    movie.setRegions(getRegions(getValues(element)));
                } else if (phase == 7) {
                    movie.setSourceTime(getSourceTime(text));
                }
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

    private Date getSourceTime(String text) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return df.parse(text);
        } catch (ParseException e) {
            logger.warn("get time failed.", e);
        }
        return new Date();
    }

    private Set<String> getValues(Element element) {
        Set<String> values = new HashSet<>();
        for (Element a : element.select("a")) {
            values.add(a.text());
        }
        return values;
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

}
