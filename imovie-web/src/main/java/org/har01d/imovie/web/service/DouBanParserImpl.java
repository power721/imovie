package org.har01d.imovie.web.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.TextUtils;
import org.har01d.imovie.util.UrlUtils;
import org.har01d.imovie.web.domain.Movie;
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
    private static final String[] tokens = new String[]{"导演:", "编剧:", "主演:", "类型:", "制片国家/地区:", "语言:", "上映日期:",
        "片长:", "又名:", "IMDb链接:", "官方网站:", "官方小站:", "首播:", "季数:", "集数:", "单集片长:"};

    @Autowired
    private MovieService service;

    @Override
    public Movie parse(Movie movie) throws IOException {
        String url = movie.getDbUrl();
        logger.info("parse {} {}", url, movie.getTitle());
        String html = HttpUtils.getHtml(url, "UTF-8");

        Document doc = Jsoup.parse(html);
        Element content = doc.select("#content").first();
        Element header = content.select("h1").first();
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

        movie.setName(fixTitle(name));
        movie.setTitle(fixTitle(header.text()));
        movie.setThumb(thumb);
        movie.setCover(getCover(thumb));
        movie.setDbScore(dbScore);
        movie.setDbUrl(url);
        movie.setSynopsis(StringUtils.truncate(findSynopsis(synopsis), 10000));
        try {
            movie.setVotes(Integer.parseInt(content.select(".rating_sum .rating_people span").text()));
        } catch (Exception e) {
            // ignore
        }

        Set<String> snapshots = new HashSet<>();
        for (Element element : content.select("#related-pic img")) {
            snapshots.add(element.attr("src").replace("photo/albumicon", "photo/photo"));
        }
        movie.setSnapshots(snapshots);

//        Set<String> tags = new HashSet<>();
//        for (Element element : content.select(".tags-body a")) {
//            tags.add(element.text());
//        }
//        movie.setTags(service.getTags(tags));

        String[] lines = handleTokens(info.text());
        for (String line : lines) {
            getMetadata(line, movie);
        }

        String season = doc.select("select#season option[selected]").text();
        if (season != null && !season.isEmpty()) {
            try {
                movie.setSeason(Integer.valueOf(season));
            } catch (NumberFormatException e) {
                movie.setSeason(0);
            }
        }

        String top250 = doc.select("div.top250 span.top250-no").text();
        if (StringUtils.isNotEmpty(top250)) {
            try {
                movie.setDb250(Integer.valueOf(top250.replace("No.", "")));
            } catch (NumberFormatException e) {
                logger.warn("Parse DouBan top 250 failed.", e);
            }
        }

        if (year != null) {
            movie.setYear(TextUtils.getYear(year));
        } else {
            movie.setYear(TextUtils.getYear(movie.getReleaseDate()));
        }

        return movie;
    }

    private String fixTitle(String text) {
        return StringUtils.truncate(text, 250);
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

    private void getMetadata(String text, Movie movie) {
        Set<String> values;
        if ((values = getValues(text, "导演:")) != null) {
            movie.setDirectors(service.getPeople(values));
            return;
        }

        if ((values = getValues(text, "编剧:")) != null) {
            movie.setEditors(service.getPeople(values));
            return;
        }

        if ((values = getValues(text, "主演:")) != null) {
            movie.setActors(service.getPeople(values));
            return;
        }

        if ((values = getValues(text, "类型:")) != null) {
            movie.setCategories(service.getCategories(values));
            return;
        }

        if ((values = getValues(text, "制片国家/地区:")) != null) {
            movie.setRegions(service.getRegions(values));
            return;
        }

        if ((values = getValues(text, "语言:")) != null) {
            movie.setLanguages(service.getLanguages(values));
            return;
        }

        if ((values = getValues(text, "又名:")) != null) {
            movie.setAliases(values);
            return;
        }

        String value;
        if ((value = getValue(text, "上映日期:", 120)) != null) {
            movie.setReleaseDate(value);
            return;
        } else if ((value = getValue(text, "首播:")) != null) {
            movie.setReleaseDate(value);
            if (movie.getEpisode() == null) {
                movie.setEpisode(0);
            }
        }

        if ((value = getValue(text, "片长:", 100)) != null) {
            movie.setRunningTime(value);
            return;
        } else if ((value = getValue(text, "单集片长:", 100)) != null) {
            movie.setRunningTime(value);
            if (movie.getEpisode() == null) {
                movie.setEpisode(0);
            }
        }

        if ((value = getValue(text, "集数:")) != null) {
            try {
                movie.setEpisode(Integer.valueOf(value));
                return;
            } catch (NumberFormatException e) {
                movie.setEpisode(0);
            }
        }

        if (getValue(text, "季数:") != null) {
            if (movie.getEpisode() == null) {
                movie.setEpisode(0);
            }
        }

        if ((value = getValue(text, "官方网站:")) != null) {
            movie.setWebsite(value);
            return;
        }

        if ((value = getValue(text, "IMDb链接:")) != null) {
            movie.setImdbUrl(UrlUtils.getImdbUrl(value));
        }
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
        text = text.trim();
        if (!text.startsWith(prefix)) {
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

}
