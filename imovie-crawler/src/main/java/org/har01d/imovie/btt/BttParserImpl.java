package org.har01d.imovie.btt;

import java.io.File;
import java.io.IOException;
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

        movie = getMovie(text);
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

    private Movie getMovie(String text) throws IOException {
        Movie movie;
        String dbUrl = getDbUrl(text);
        if (dbUrl != null) {
            movie = service.findByDbUrl(dbUrl);
            if (movie == null) {
                movie = douBanParser.parse(dbUrl);
            }
            return movie;
        }

        String imdb = getImdbUrl(text);
        if (imdb != null && (movie = service.findByImdb(imdb)) != null) {
            return movie;
        }

        return null;
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
        if (index < 0) {
            return null;
        }
        String text = html.substring(index - "http://".length(), index + 40);
        Matcher matcher = IMDB_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
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
