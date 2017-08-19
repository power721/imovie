package org.har01d.imovie.mj;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MjParserImpl extends AbstractParser implements MjParser {

    private static final Logger logger = LoggerFactory.getLogger(MjParser.class);

    @Value("${url.mj.site}")
    private String siteUrl;

    @Override
    @Transactional
    public Movie parse(String url, Movie movie) throws IOException {
        Movie m;
        if (movie.getId() != null) {
            m = service.findById(movie.getId());
        } else {
            getMovie(url.replace("www.", "m."), movie);
            m = searchByName(movie);
        }

        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);

        if (m != null) {
            Set<Resource> resources = m.getRes();
            int size = resources.size();
            resources.addAll(findResource(doc, movie.getName()));

            logger.info("[mj] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
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
        Elements elements = doc.select("div.block ul.downloadlist li.down-item a.down-link");
        for (Element element : elements) {
            String uri = element.attr("href");
            if (uri.startsWith("d2k://")) {
                uri = "e" + uri;
            }
            if (isResource(uri)) {
                String title = element.text();
                String temp = element.parent().parent().parent().select("h3 span a.current").text();
                if (!temp.isEmpty() && !"下载".equals(temp)) {
                    title = temp + "-" + title;
                }
                if (!title.contains(name)) {
                    title = name + "-" + title;
                }
                try {
                    if (uri.startsWith("thunder://")) {
                        resources
                            .add(service.saveResource(UrlUtils.convertUrl(uri), uri, StringUtils.truncate(title, 120)));
                    } else {
                        resources.add(service.saveResource(uri, StringUtils.truncate(title, 120)));
                    }
                } catch (Exception e) {
                    service.publishEvent(name, e.getMessage());
                    logger.error("[mj] get resource failed", e);
                }
            } else if (!uri.trim().isEmpty()) {
                logger.warn("{} is not resource", uri);
            }
        }

        return resources;
    }

    private void getMovie(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);
        movie.setTitle(doc.select("div.wrapper div.detailPosterIntro div.introTxt h1").text());
        if (movie.getName() == null) {
            movie.setName(movie.getTitle());
        }
        movie.setAliases(getValues(movie.getTitle()));

        Elements elements = doc.select("div.wrapper div.detailPosterIntro div.introTxt .intro span");
        for (Element element : elements) {
            String text = element.text();
            if (text.contains("地区：")) {
                String temp = text;
                int index = temp.indexOf("地区：") + "地区：".length();
                temp = temp.substring(index, temp.length());

                movie.setRegions(getRegions(getValues1(temp)));
            } else if (text.contains("类型：")) {
                String temp = text;
                int index = temp.indexOf("类型：") + "类型：".length();
                temp = temp.substring(index, temp.length());

                movie.setCategories(getCategories(getValues2(temp)));
            } else if (text.contains("语言： ")) {
                String temp = text;
                int index = temp.indexOf("语言： ") + "语言： ".length();
                temp = temp.substring(index, temp.length());

                movie.setLanguages(getLanguages(getValues3(temp)));
            } else if (text.contains("年代：")) {
                movie.setYear(service.getYear(text));
            } else if (text.contains("导演：")) {
                movie.setDirectors(getPeople(getValues(element)));
            } else if (text.contains("主演：")) {
                movie.setActors(getPeople(getValues(element)));
            } else if (text.contains("本季终") || text.contains("本剧完结")) {
                movie.setCompleted(true);
            }
        }

        movie.setSynopsis(doc.select("p#movie_info_intro_s").text().replace("剧情简介：", "").trim());
        if (!movie.getName().contains("第") && !movie.getName().endsWith("季")) {
            Matcher matcher = NAME.matcher(movie.getTitle());
            if (matcher.matches()) {
                movie.setName(movie.getName() + " " + matcher.group(2));
            }
        }
    }

    private Set<String> getValues(Element element) {
        Set<String> values = new HashSet<>();
        for (Element a : element.select("a")) {
            String name = a.text().trim();
            if ("未录入".equals(name)) {
                continue;
            }
            if (!name.isEmpty()) {
                values.add(name);
            }
        }
        return values;
    }

    private Set<String> getValues(String text) {
        Set<String> values = new HashSet<>();
        String[] temp = text.split("/");
        if (temp.length > 1) {
            for (int i = 1; i < temp.length; ++i) {
                String name = temp[i].trim();
                if (!name.isEmpty()) {
                    values.add(name);
                }
            }
        }
        return values;
    }

    private Set<String> getValues1(String text) {
        Set<String> values = new HashSet<>();
        for (String name : text.split(" ")) {
            name = name.trim();
            if ("未录入".equals(name)) {
                continue;
            }
            if ("大陆".equals(name)) {
                name = "中国大陆";
            }
            if (!name.isEmpty()) {
                values.add(name);
            }
        }
        return values;
    }

    private Set<String> getValues2(String text) {
        Set<String> values = new HashSet<>();
        for (String name : text.split(" ")) {
            if ("欧美剧".equals(name)) {
                continue;
            }
            name = name.replace("电影", "").trim();
            if (!name.isEmpty()) {
                values.add(name);
            }
        }
        return values;
    }

    private Set<String> getValues3(String text) {
        Set<String> values = new HashSet<>();
        for (String name : text.split("[ ，]")) {
            if (name.contains("字幕") || "未录入".equals(name)) {
                continue;
            }
            name = name.replace("对白", "").replace("发音", "").trim();
            if ("国语".equals(name) || "普通话".equals(name) || "国语中字".equals(name)) {
                name = "汉语普通话";
            }
            if (!name.isEmpty()) {
                values.add(name);
            }
        }
        return values;
    }

}
