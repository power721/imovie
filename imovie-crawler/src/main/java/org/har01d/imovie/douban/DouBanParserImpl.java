package org.har01d.imovie.douban;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.service.MovieService;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DouBanParserImpl implements DouBanParser {

    private static final Logger logger = LoggerFactory.getLogger(DouBanParser.class);
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})-\\d{2}-\\d{2}");
    private static final String[] tokens = new String[]{"导演:", "编剧:", "主演:", "类型:", "制片国家/地区:", "语言:", "上映日期:",
        "片长:", "又名:", "IMDb链接:", "官方网站:", "首播:", "季数:", "集数:", "单集片长:"};

    @Autowired
    private MovieService service;

    @Override
    public Movie parse(String url) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);
        Element content = doc.select("#content").first();
        Element header = content.select("h1").first();
        logger.info("parse {} {}", url, header.text());
        String name = header.child(0).text();
        String year = null;
        if (header.children().size() > 1) {
            year = header.child(1).text();
            year = year.substring(1, year.length() - 1);
        }
        String dbScore = content.select(".rating_num").text();
        Element subject = content.select(".subject").first();
        String thumb = subject.select("#mainpic img").attr("src");
        Element info = subject.select("#info").first();
        Element synopsis = content.select(".related-info #link-report").first();

        Movie movie = new Movie();
        movie.setName(name);
        movie.setTitle(header.text());
        movie.setThumb(thumb);
        movie.setCover(getCover(thumb));
        movie.setDbScore(dbScore);
        movie.setDbUrl(url);
        movie.setSynopsis(findSynopsis(synopsis));

        Set<String> snapshots = new HashSet<>();
        for (Element element : content.select("#related-pic img")) {
            snapshots.add(element.attr("src").replace("photo/albumicon", "photo/photo"));
        }
        movie.setSnapshots(snapshots);

        Set<String> tags = new HashSet<>();
        for (Element element : content.select(".tags-body a")) {
            tags.add(element.text());
        }
        movie.setTags(service.getTags(tags));

        String[] lines = handleTokens(info.text());
        for (String line : lines) {
            getMetadata(line, movie);
        }
        if (year != null) {
            getYear(movie, year);
        } else {
            getYear(movie, movie.getReleaseDate());
        }

        return movie;
    }

    private String getCover(String url) {
        return url.replace("movie_poster_cover/lpst", "photo/photo");
    }

    private String[] handleTokens(String text) {
        for (String token : tokens) {
            text = text.replace(" " + token, "\n" + token);
        }
        return text.split("\n");
    }

    private String findSynopsis(Element synopsis) {
        if (synopsis == null) {
            return null;
        }

        for (Element element : synopsis.children()) {
            String text = element.text();
            if (!text.contains("(展开全部)") && !text.contains("©豆瓣") && text.length() > 10) {
                return text;
            }
        }
        return synopsis.text();
    }

    private boolean getMetadata(String text, Movie movie) {
        Set<String> values;
        if ((values = getValues(text, "导演:")) != null) {
            movie.setDirectors(service.getPersons(values));
            return true;
        }

        if ((values = getValues(text, "编剧:")) != null) {
            movie.setEditors(service.getPersons(values));
            return true;
        }

        if ((values = getValues(text, "主演:")) != null) {
            movie.setActors(service.getPersons(values));
            return true;
        }

        if ((values = getValues(text, "类型:")) != null) {
            movie.setCategories(service.getCategories(values));
            return true;
        }

        if ((values = getValues(text, "制片国家/地区:")) != null) {
            movie.setRegions(service.getRegions(values));
            return true;
        }

        if ((values = getValues(text, "语言:")) != null) {
            movie.setLanguages(service.getLanguages(values));
            return true;
        }

        if ((values = getValues(text, "又名:")) != null) {
            movie.setAliases(values);
            return true;
        }

        String value;
        if ((value = getValue(text, "上映日期:", 120)) != null) {
            movie.setReleaseDate(value);
            return true;
        } else if ((value = getValue(text, "首播:")) != null) {
            movie.setReleaseDate(value);
        }

        if ((value = getValue(text, "片长:", 100)) != null) {
            movie.setRunningTime(value);
            return true;
        } else if ((value = getValue(text, "单集片长:", 100)) != null) {
            movie.setRunningTime(value);
        }

        if ((value = getValue(text, "IMDb链接:")) != null) {
            movie.setImdbUrl(getImdbUrl(value));
            return true;
        }
        return false;
    }

    private String getValue(String text, String prefix) {
        if (!text.trim().startsWith(prefix)) {
            return null;
        }
        return text.substring(prefix.length(), text.length()).trim();
    }

    private String getValue(String text, String prefix, int maxLen) {
        String result = getValue(text, prefix);
        if (result != null) {
            return StringUtils.truncate(result, maxLen);
        } else {
            return null;
        }
    }

    private Set<String> getValues(String text, String prefix) {
        if (!text.trim().startsWith(prefix)) {
            return null;
        }

        Set<String> values = new HashSet<>();
        String value = text.substring(prefix.length(), text.length());
        String regex = " / ";
        String[] vals = value.split(regex);
        for (String val : vals) {
            values.add(val.trim());
        }

        return values;
    }

    private void getYear(Movie movie, String yearStr) {
        if (yearStr == null) {
            return;
        }

        Matcher matcher = DATE_PATTERN.matcher(yearStr);
        if (matcher.find()) {
            int year = Integer.valueOf(matcher.group(1));
            movie.setYear(year);
        } else if (yearStr.matches("\\d{4}")) {
            int year = Integer.valueOf(yearStr);
            movie.setYear(year);
        }
    }

    private String getImdbUrl(String imdb) {
        if (imdb.contains("http://www.imdb.com/title/")) {
            return imdb;
        }
        return "http://www.imdb.com/title/" + imdb;
    }

}