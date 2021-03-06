package org.har01d.imovie.douban;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.apache.http.impl.client.BasicCookieStore;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Source;
import org.har01d.imovie.service.DouBanService;
import org.har01d.imovie.service.MovieService;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.UrlUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

    @Autowired
    private BasicCookieStore cookieStore;

    @Autowired
    private DouBanService douBanService;

    @Autowired
    private DouBanListParser douBanListParser;

    private volatile int errorCount;
    private volatile int count;

    @Override
    public Movie parse(String url) throws IOException {
        return parse(url, false);
    }

    @Override
    public synchronized Movie parse(String url, boolean includeDbList) throws IOException {
        if (count++ > 0) {
            if (count % 100 == 0) {
                douBanService.updateCookie();
            } else if (count % 1000 == 0) {
                douBanService.tryLogin();
            }
        }

        String html = getHtml(url);

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
            movie.setYear(service.getYear(year));
        } else {
            movie.setYear(service.getYear(movie.getReleaseDate()));
        }

        if (includeDbList) {
            service.updateMovie(url, movie);
            parseDouList(doc, movie.getTitle());
        }
        return movie;
    }

    private String getHtml(String url) throws IOException {
        String html;
        try {
            html = HttpUtils.getHtml(url, "UTF-8");
            errorCount = 0;
            return html;
        } catch (HttpResponseException e) {
            handle403(e);
            if (e.getStatusCode() != 404 || !douBanService.isLogin()) {
                throw e;
            }
        }

        try {
            html = HttpUtils.getHtml(url, "UTF-8", cookieStore);
            errorCount = 0;
        } catch (HttpResponseException e) {
            handle403(e);
            throw e;
        }
        return html;
    }

    @Override
    public synchronized List<Movie> search(String text) throws IOException {
        String url;
        List<Movie> movies = new ArrayList<>();
        try {
            url = "https://m.douban.com/search/?type=movie&query=" + URLEncoder.encode(text, "UTF-8");
//            url = "https://movie.douban.com/subject_search?search_text=" + URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error("search movie failed: " + text, e);
            return movies;
        }

        String html = getHtml(url);

        Document doc = Jsoup.parse(html);
        Elements elements = doc.select("ul.search_results_subjects li a");

        for (Element element : elements) {
            String dbUrl = element.attr("href");
            if (dbUrl.contains("/movie/subject/")) {
                dbUrl = "https://movie.douban.com" + dbUrl.replace("/movie", "");
                Movie m = service.findByDbUrl(dbUrl);
                if (m != null) {
                    movies.add(m);
                }

                if (m == null) {
                    try {
                        m = parse(dbUrl);
                        service.save(new Source(dbUrl));
                        if (m != null) {
                            movies.add(service.save(m));
                        }
                    } catch (Exception e) {
                        service.publishEvent(dbUrl, e.getMessage());
                        logger.error("Parse page failed: " + dbUrl, e);
                    }
                }
            }
        }
        return movies;
    }

    private void handle403(HttpResponseException e) {
        if (e.getStatusCode() == 403) {
            if (errorCount == 0) {
                douBanService.reLogin();
            }
            if (errorCount++ >= 3) {
                throw new Error("403 Forbidden", e);
            }
        }
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
            if (movie.getEpisode() == null) {
                movie.setEpisode(0);
            }
        }

        if ((value = getValue(text, "片长:", 100)) != null) {
            movie.setRunningTime(value);
            return true;
        } else if ((value = getValue(text, "单集片长:", 100)) != null) {
            movie.setRunningTime(value);
            if (movie.getEpisode() == null) {
                movie.setEpisode(0);
            }
        }

        if ((value = getValue(text, "集数:")) != null) {
            try {
                movie.setEpisode(Integer.valueOf(value));
                return true;
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
            return true;
        }

        if ((value = getValue(text, "IMDb链接:")) != null) {
            movie.setImdbUrl(UrlUtils.getImdbUrl(value));
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
        if (vals.length == 1 && value.contains("/")) {
            vals = value.split("/");
        }

        for (String val : vals) {
            values.add(val.trim());
        }

        return values;
    }

    private void parseDouList(Document doc, String title) {
        Elements elements = doc.select("div#subject-doulist ul li a");
        for (Element element : elements) {
            String url = element.attr("href");
            if (service.findSource(url) == null) {
                try {
                    logger.info("{}:{}", title, url);
                    douBanListParser.parse(url);
                } catch (IOException | ParseException e) {
                    logger.warn("parse DouBan list failed.", e);
                }
            }
        }
    }

}
