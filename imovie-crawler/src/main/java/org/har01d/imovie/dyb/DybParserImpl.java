package org.har01d.imovie.dyb;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;
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
public class DybParserImpl extends AbstractParser implements DybParser {

    private static final Logger logger = LoggerFactory.getLogger(DybParser.class);

    @Value("${url.dyb.site}")
    private String siteUrl;

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

            logger.info("[dyb] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
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
        Elements elements = doc.select("div.down_list ul li a");
        for (Element element : elements) {
            String uri = element.attr("href");
            if (isResource(uri)) {
                String title = element.text();
                try {
                    resources
                        .add(service.saveResource(UrlUtils.convertUrl(uri), uri, StringUtils.truncate(title, 120)));
                } catch (Exception e) {
                    service.publishEvent(name, e.getMessage());
                    logger.error("[dyb] get resource failed", e);
                }
            } else if (!uri.trim().isEmpty()) {
                logger.warn("{} is not resource", uri);
            }
        }

        elements = doc.select("div.o_cn2 ul li a");
        for (Element element : elements) {
            String uri = element.attr("href");
            String text = element.text();
            if (uri.startsWith("/player/") && text.contains("密码")) {
                resources.addAll(findResource(siteUrl + uri, text));
            }
        }
        return resources;
    }

    private Set<Resource> findResource(String url, String title) {
        Set<Resource> resources = new HashSet<>();
        try {
            String html = HttpUtils.getHtml(url);
            Document doc = Jsoup.parse(html);
            String uri = doc.select("div.kp_flash_box .explaywrap a.explaylink").attr("href");
            if (isResource(uri)) {
                resources.add(service.saveResource(uri, StringUtils.truncate(title, 120)));
            }
        } catch (Exception e) {
            service.publishEvent(url, e.getMessage());
            logger.error("[dyb] get resource failed", e);
        }
        return resources;
    }

    private void getMovie(Document doc, Movie movie) {
        Elements elements = doc.select("meta");
        for (Element element : elements) {
            String property = element.attr("property");
            String content = element.attr("content");
            if ("og:title".equals(property)) {
                movie.setTitle(content);
                if (movie.getName() == null) {
                    movie.setName(content);
                }
                movie.setAliases(getValues2(content));
            } else if ("og:video:actor".equals(property)) {
                movie.setActors(getPeople(getValues(content)));
            } else if ("og:video:area".equals(property)) {
                movie.setRegions(getRegions(getValues(content)));
            } else if ("og:video:class".equals(property)) {
                movie.setRegions(getRegions(getValues1(content)));
            }
        }

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm");
        elements = doc.select("div.o_r_wap .o_r_t_wap3 .o_r_contact ul li");
        for (Element element : elements) {
            String text = element.text();
            if (text.contains("上映：")) {
                movie.setYear(service.getYear(text));
            }
            if (text.contains("全集")) {
                movie.setCompleted(true);
            }
            if (text.contains("更新时间：")) {
                int index = text.indexOf("更新时间：") + "更新时间：".length();
                String temp = text.substring(index, index + "yyyy-MM-dd hh:mm".length());
                try {
                    movie.setSourceTime(df.parse(temp));
                } catch (ParseException e) {
                    // ignore
                }
            }
        }

        elements = doc.select("div.o_r_t_wap2 .omov_list3 div");
        boolean metadata = false;
        for (Element element : elements) {
            String text = element.text();
            if (text.contains("导演:")) {
                movie.setDirectors(getPeople(getValues(text, "导演:")));
                metadata = true;
            } else if (text.contains("编剧:")) {
                movie.setEditors(getPeople(getValues(text, "编剧:")));
                metadata = true;
            } else if (text.contains("主演:")) {
                movie.setActors(getPeople(getValues(text, "主演:")));
                metadata = true;
            } else if (text.contains("类型:")) {
                movie.setCategories(getCategories(getValues(text, "类型:")));
                metadata = true;
            } else if (text.contains("首播:")) {
                movie.setReleaseDate(getValue(text, "首播:"));
                metadata = true;
            } else if (text.contains("上映日期:")) {
                movie.setReleaseDate(getValue(text, "上映日期:"));
                metadata = true;
            } else if (text.contains("片长:")) {
                movie.setRunningTime(getValue(text, "片长:"));
                metadata = true;
            } else if (text.contains("IMDb链接:")) {
                movie.setImdbUrl(UrlUtils.getImdbUrl(text));
                metadata = true;
            } else if (text.contains("集数:")) {
                String value = getValue(text, "集数:");
                metadata = true;
                if (value != null) {
                    try {
                        movie.setEpisode(Integer.valueOf(value));
                    } catch (NumberFormatException e) {
                        movie.setEpisode(0);
                    }
                }
            } else if (text.contains("语言:")) {
                movie.setLanguages(getLanguages(getValues(text, "语言:")));
                metadata = true;
            } else if (text.contains("又名:")) {
                movie.setAliases(getValues(text, "又名:"));
                metadata = true;
            } else if (text.contains("制片国家/地区:")) {
                movie.setRegions(getRegions(getValues(text, "制片国家/地区:")));
                metadata = true;
            } else if (metadata && !text.substring(0, 6).contains(":")) {
                movie.setSynopsis(text);
            }
        }
    }

    private String getValue(String text, String prefix) {
        if (!text.trim().startsWith(prefix)) {
            return null;
        }
        return text.substring(prefix.length(), text.length()).trim();
    }

    private Set<String> getValues(String text, String prefix) {
        if (!text.trim().startsWith(prefix)) {
            return null;
        }

        Set<String> values = new HashSet<>();
        String value = text.substring(prefix.length(), text.length());
        String regex = " / ";
        String[] vals = value.split(regex);
        if (vals.length == 1 && value.contains("/")) {
            vals = value.split("/");
        }

        for (String val : vals) {
            values.add(val.trim());
        }

        return values;
    }

    private Set<String> getValues(String text) {
        Set<String> values = new HashSet<>();
        for (String name : text.split(",")) {
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

    private Set<String> getValues1(String text) {
        Set<String> values = new HashSet<>();
        String[] temp = text.split("/");
        if (temp.length == 2) {
            for (String name : temp[1].split(",")) {
                name = name.trim();
                if (!name.isEmpty()) {
                    values.add(name);
                }
            }
        }
        return values;
    }

    private Set<String> getValues2(String text) {
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

}
