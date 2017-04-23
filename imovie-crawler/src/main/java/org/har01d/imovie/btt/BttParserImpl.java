package org.har01d.imovie.btt;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.bt.BtUtils;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.douban.DouBanParser;
import org.har01d.imovie.service.MovieService;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.StringUtils;
import org.har01d.imovie.util.UrlUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BttParserImpl implements BttParser {

    private static final Logger logger = LoggerFactory.getLogger(BttParser.class);
    private static final Pattern DB_PATTERN = Pattern.compile("(https?://movie\\.douban\\.com/subject/\\d+/)");
    private static final Pattern IMDB_PATTERN = Pattern.compile("(https?://www\\.imdb\\.com/title/tt\\d+)");
    private static final Pattern IMDB = Pattern.compile("(tt\\d+)");

    private static final String[] TOKENS = new String[]{"导演:", "编剧:", "主演:", "类型:", "制片国家/地区:", "语言:", "上映日期:",
        "片长:", "又名:", "IMDb链接:", "官方网站:", "首播:", "季数:", "集数:", "单集片长:"};

    @Value("${url.btt.site}")
    private String siteUrl;

    @Value("${file.download}")
    private File downloadDir;

    private int id;

    @Autowired
    private DouBanParser douBanParser;

    @Autowired
    private MovieService service;

    @Override
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);
        String text = doc.select("div.post").text();

        movie = getMovie(text, movie);
        if (movie == null) {
            logger.warn("Cannot find movie for " + url);
            service.publishEvent(url, "Cannot find movie");
            return null;
        }

        Set<Resource> resources = movie.getResources();
        Elements elements = doc.select("div.post p");
        for (Element element : elements) {
            findResource(element.text(), resources);
        }

        findResource(doc, resources);
        findAttachments(doc, resources);

        logger.info("get {} resources for movie {}", resources.size(), movie.getName());
        service.save(movie);
        return movie;
    }

    private Movie getMovie(String text, Movie movie) throws IOException {
        String dbUrl = getDbUrl(text);
        if (dbUrl != null) {
            movie = service.findByDbUrl(dbUrl);
            if (movie == null) {
                movie = douBanParser.parse(dbUrl);
            }
            return movie;
        }

        String imdb = getImdbUrl(text);
        if (imdb != null) {
            Movie m = service.findByImdb(imdb);
            if (m != null) {
                return m;
            }
        }

        getMetadata(text, movie);
        return findMovie(movie);
    }

    private void getMetadata(String text, Movie movie) {
        if (text.contains("◎类　　别　") || text.contains("◎片　　名　")) {
            int start = text.indexOf("◎国　　家　") + 6;
            int end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setRegions(service.getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎类　　别　") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setCategories(service.getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎语　　言　") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setLanguages(service.getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎上映日期　") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setReleaseDate(StringUtils.truncate(text.substring(start, end), 120));
            }

            start = text.indexOf("◎译　　名　") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setAliases(getValues(text.substring(start, end)));
            }
        } else {
            int start = text.indexOf("制片国家/地区:") + 8;
            int end = getNextToken(text, start);
            if (start > 10 && end > start) {
                movie.setRegions(service.getRegions(getValues2(text.substring(start, end))));
            }

            start = text.indexOf("类型:") + 3;
            end = getNextToken(text, start);
            if (start > 10 && end > start) {
                movie.setCategories(service.getCategories(getValues2(text.substring(start, end))));
            }

            start = text.indexOf("语言:") + 3;
            end = getNextToken(text, start);
            if (start > 10 && end > start) {
                movie.setLanguages(service.getLanguages(getValues2(text.substring(start, end))));
            }

            start = text.indexOf("上映日期:") + 5;
            end = getNextToken(text, start);
            if (start > 10 && end > start) {
                movie.setReleaseDate(StringUtils.truncate(text.substring(start, end), 120));
            }

            start = text.indexOf("又名:") + 3;
            end = getNextToken(text, start);
            if (start > 10 && end > start) {
                movie.setLanguages(service.getLanguages(getValues2(text.substring(start, end))));
            }
        }
    }

    private int getNextToken(String text, int start) {
        int index = text.indexOf(" / ", start);
        for (String token : TOKENS) {
            int i = text.indexOf(token, start);
            if (i > 0 && i < index) {
                index = i;
            }
        }
        return index;
    }

    private Set<String> getValues(String text) {
        Set<String> values = new HashSet<>();
        String regex = "/";
        String[] vals = text.split(regex);
        for (String val : vals) {
            values.add(val.replaceAll("　", "").trim());
        }

        return values;
    }

    private Set<String> getValues2(String text) {
        Set<String> values = new HashSet<>();
        String regex = " / ";
        String[] vals = text.split(regex);
        for (String val : vals) {
            values.add(val.trim());
        }

        return values;
    }

    private Movie findMovie(Movie movie) {
        String name = movie.getName();
        if (name == null) {
            return null;
        }

        List<Movie> movies = service.findByName(name);
        Movie best = null;
        int maxMatch = 0;
        for (Movie m : movies) {
            int match = 0;
            if (movie.getYear() != null) {
                if (movie.getYear().equals(m.getYear())) {
                    match++;
                }
            }

            if (movie.getCategories() != null && !movie.getCategories().isEmpty() && m.getCategories() != null) {
                if (m.getCategories().containsAll(movie.getCategories())) {
                    match++;
                }
            }

            if (movie.getRegions() != null && !movie.getRegions().isEmpty() && m.getRegions() != null) {
                if (m.getRegions().containsAll(movie.getRegions())) {
                    match++;
                }
            }

            if (movie.getLanguages() != null && !movie.getLanguages().isEmpty() && m.getLanguages() != null) {
                if (m.getLanguages().containsAll(movie.getLanguages())) {
                    match++;
                }
            }

            if (movie.getAliases() != null && !movie.getAliases().isEmpty() && m.getAliases() != null) {
                if (m.getAliases().containsAll(movie.getAliases())) {
                    match++;
                }
            }

            if (movie.getReleaseDate() != null && m.getReleaseDate() != null) {
                if (m.getReleaseDate().contains(movie.getReleaseDate())) {
                    match++;
                }
            }

            if (match > maxMatch) {
                maxMatch = match;
                best = m;
            }
        }
        return best;
    }

    private String getDbUrl(String html) {
        int index = html.indexOf("movie.douban.com/subject/");
        if (index < 0) {
            return null;
        }
        String text = html.substring(index - "https://".length(), index + 45);
        Matcher matcher = DB_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String getImdbUrl(String html) {
        int index = html.indexOf("www.imdb.com/title/");
        if (index > 0) {
            String text = html.substring(index - "http://".length(), index + 40);
            Matcher matcher = IMDB_PATTERN.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        index = html.indexOf("IMDb链接");
        if (index > 0) {
            String text = html.substring(index + 5, index + 25);
            Matcher matcher = IMDB.matcher(text);
            if (matcher.find()) {
                return "http://www.imdb.com/title/" + matcher.group(1);
            }
        }

        return null;
    }

    private void findResource(Document doc, Set<Resource> resources) {
        Elements elements = doc.select(".post a");
        for (Element element : elements) {
            String uri = element.attr("href");
            if (isResource(uri)) {
                String title = element.parent().text();
                resources.add(service.saveResource(uri, title));
            }
        }
    }

    private void findResource(String text, Set<Resource> resources) {
        String magnet = UrlUtils.findMagnet(text);
        if (magnet != null) {
            resources.add(service.saveResource(magnet, StringUtils.truncate(magnet, 100)));
        }

        String ed2k = UrlUtils.findED2K(text);
        if (ed2k != null) {
            resources.add(service.saveResource(ed2k, StringUtils.truncate(ed2k, 100)));
        }
    }

    private void findAttachments(Document doc, Set<Resource> resources) {
        Elements elements = doc.select(".attachlist a");
        for (Element element : elements) {
            String href = element.attr("href");
            if (href.startsWith("attach-dialog-fid-")) {
                String title = element.text();
                String uri = siteUrl + href.replace("-dialog-", "-download-");
                String magnet = convertTorrent(uri, title);
                resources.add(service.saveResource(magnet, uri, title));
            }
        }
    }

    private String convertTorrent(String uri, String title) {
        String name = (id++ / 20) + ".torrent";
        File file = new File(downloadDir, name);
        try {
            downloadDir.mkdirs();
            file.createNewFile();
            HttpUtils.downloadFile(uri, file);
            String magnet = BtUtils.torrent2Magnet(file);
            logger.info("convert {} to {}", title, magnet);
            return magnet;
        } catch (Exception e) {
            logger.error("convert torrent to magnet failed: " + title, e);
            service.publishEvent(uri, "convert torrent to magnet failed: " + title);
        }
        return null;
    }

    private boolean isResource(String uri) {
        return uri.startsWith("magnet") || uri.startsWith("ed2k://") || uri.startsWith("thunder://")
            || uri.startsWith("ftp://") || uri.contains("pan.baidu.com");
    }

}
