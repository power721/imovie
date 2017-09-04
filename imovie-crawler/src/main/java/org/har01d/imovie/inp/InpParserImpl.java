package org.har01d.imovie.inp;

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
import org.har01d.imovie.util.TextUtils;
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
public class InpParserImpl extends AbstractParser implements InpParser {

    private static final Logger logger = LoggerFactory.getLogger(InpParser.class);
    private static final Pattern EP = Pattern.compile("(\\d+)集");

    @Value("${url.inp.site}")
    private String siteUrl;

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

            String dbUrl = movie.getDbUrl();
            if (dbUrl != null) {
                m = getByDb(dbUrl);
            }

            if (m == null) {
                String imdb = movie.getImdbUrl();
                if (imdb != null) {
                    m = service.findByImdb(imdb);
                }
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
            m.addResources(findResource(doc, movie.getName()));

            logger.info("[inp] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
            m.setNewResources(resources.size() - size);
            m.setCompleted(movie.isCompleted());
            m.setSourceTime(movie.getSourceTime());
            service.save(m);
            return m;
        } else {
            Set<Resource> resources = findResource(doc, movie.getName());

            logger.warn("Cannot find movie for {}:{} {}", movie.getName(), resources.size(), url);
            service.publishEvent(url, "Cannot find movie for " + movie.getName());
            return null;
        }
    }

    private Set<Resource> findResource(Document doc, String name) {
        Set<Resource> resources = new HashSet<>();
        if (skipResource) {
            return resources;
        }

        Elements elements = doc.select("div.downbox .zylistbox table tr td.td_thunder");
        for (Element element : elements) {
            String uri = element.select("input").val();
            if (uri.isEmpty()) {
                uri = element.select("a").attr("href");
                if (uri.startsWith("/down/")) {
                    resources.addAll(findResource(siteUrl + uri, element.text()));
                }
            } else if (isResource(uri)) {
                String title = element.select("a").text();
                if (!title.contains(name)) {
                    title = name + "-" + title;
                }
                try {
                    resources
                        .add(service.saveResource(UrlUtils.convertUrl(uri), uri, TextUtils.truncate(title, 120)));
                } catch (Exception e) {
                    service.publishEvent(name, e.getMessage());
                    logger.error("[inp] get resource failed", e);
                }
            }
        }

        elements = doc.select("div.moiveinfobox .movieall ul.movieplaylist li a");
        for (Element element : elements) {
            String uri = element.attr("href");
            if (uri.startsWith("/down/")) {
                String title = element.text();
                if (!title.contains(name)) {
                    title = name + "-" + title;
                }
                resources.addAll(findResource(siteUrl + uri, title));
            }
        }
        return resources;
    }

    private Set<Resource> findResource(String url, String title) {
        Set<Resource> resources = new HashSet<>();
        try {
            String html = HttpUtils.getHtml(url);
            Document doc = Jsoup.parse(html);
            String uri = doc.select("a#down_cili").attr("href");
            if (uri.isEmpty()) {
                uri = doc.select("iframe#play_area").attr("src");
                if (isResource(uri)) {
                    resources.add(service.saveResource(uri, TextUtils.truncate(title, 120)));
                } else if (doc.select("h3 a").text().contains("中文字幕")) {
                    uri = doc.select("div#down_verify_box li a.btn-orange").attr("href");
                    title = "中文字幕-" + title;
                    resources.add(service.saveResource(uri, TextUtils.truncate(title, 120)));
                }
            } else if (isResource(uri)) {
                String original = siteUrl + doc.select("div#down_verify_box li a.btn-orange").attr("href");
                resources.add(service.saveResource(uri, original, TextUtils.truncate(title, 120)));
            }
        } catch (Exception e) {
            service.publishEvent(url, e.getMessage());
            logger.error("[inp] get resource failed", e);
        }
        return resources;
    }

    private void getMovie(Document doc, Movie movie) {
        movie.setTitle(doc.select("div.moiveinfobox h2 a").text());
        if (movie.getName() == null) {
            movie.setName(movie.getTitle());
        }

        Elements elements = doc.select("div.moiveinfobox div.minfocon div.moiveinfo ul.minfolist li");
        for (Element element : elements) {
            String text = element.text();
            if (text.contains("地区：")) {
                String temp = text;
                int index = temp.indexOf("地区：") + "地区：".length();
                temp = temp.substring(index, temp.length());

                movie.setRegions(getRegions(getValues(temp)));
            }
            if (text.contains("年代：")) {
                movie.setYear(service.getYear(text));
            }
            if (text.contains("集数：")) {
                movie.setEpisode(getEpisode(text));
            }
            if (text.contains("导演：")) {
                movie.setDirectors(getPeople(getValues(element)));
            }
        }

        movie.setActors(
            getPeople(getValues(doc.select("div.moiveinfobox div.minfocon div.moiveinfo .yyinfo .yycon").first())));
        movie.setAliases(getValues(doc.select("div.moiveinfobox h2 .updateinfo").text()));
        movie.setSynopsis(
            doc.select("div.moiveinfobox div.minfocon div.moiveinfo .yyinfo .yycon").last().text().replace("…", ""));

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String date = doc.select("span[itemprop=datePublished]").attr("content");
        try {
            movie.setSourceTime(df.parse(date));
        } catch (ParseException e) {
            // ignore
        }

        String html = doc.select("div.moiveintrocon").html();
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
        }
        return null;
    }

    protected Set<String> getValues(Element element) {
        Set<String> values = new HashSet<>();
        for (Element a : element.select("a")) {
            String name = a.text().trim();
            if (!name.isEmpty()) {
                if ("言情".equals(name)) {
                    name = "爱情";
                }
                values.add(name);
            }
        }
        return values;
    }

    protected Set<String> getValues(String text) {
        Set<String> values = new HashSet<>();
        for (String name : text.split("/|,")) {
            name = name.trim();
            if ("未录入".equals(name)) {
                continue;
            }
            if ("大陆".equals(name)) {
                name = "中国大陆";
            } else if ("印度 India".equals(name)) {
                name = "印度";
            } else if ("荷兰 The Net".equals(name)) {
                name = "荷兰";
            } else if ("加拿大 Canada".equals(name)) {
                name = "加拿大";
            } else if ("UK".equals(name)) {
                name = "英国";
            } else if ("USA".equals(name)) {
                name = "美国";
            }
            if (!name.isEmpty()) {
                values.add(name);
            }
        }
        return values;
    }

}
