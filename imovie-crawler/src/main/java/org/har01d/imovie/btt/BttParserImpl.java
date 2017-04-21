package org.har01d.imovie.btt;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.bt.BtUtils;
import org.har01d.imovie.domain.Event;
import org.har01d.imovie.domain.EventRepository;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.MovieRepository;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.domain.ResourceRepository;
import org.har01d.imovie.douban.DouBanParser;
import org.har01d.imovie.util.HttpUtils;
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
    private static final Pattern DB_PATTERN = Pattern.compile("(https?://movie.douban.com/subject/\\d+)");

    @Value("${url.btt.site}")
    private String siteUrl;

    @Value("${file.download}")
    private String downloadDir;

    private int id;

    @Autowired
    private DouBanParser douBanParser;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Override
    public void parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);
        String dbUrl = getDbUrl(html);
        if (dbUrl != null) {
            movie = douBanParser.parse(dbUrl);
        } else {
            Elements elements = doc.select("div.post p");

            for (Element element : elements) {
                logger.debug(element.tagName());
            }
        }

        Set<Resource> resources = movie.getResources();
        findResource(doc, resources);
        findAttachments(doc, resources);

        logger.info("get {} resources for movie {}", resources.size(), movie.getName());
        movieRepository.save(movie);
    }

    private String getDbUrl(String html) {
        int index = html.indexOf("movie.douban.com/subject/");
        if (index < 0) {
            return null;
        }
        String text = html.substring(index - "https://".length(), index + 35);
        Matcher matcher = DB_PATTERN.matcher(text);
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
                Resource resource = resourceRepository.findFirstByUri(uri);
                if (resource != null) {
                    resources.add(resource);
                    continue;
                }

                resource = new Resource(uri, title);
                resourceRepository.save(resource);
                resources.add(resource);
                logger.debug("find new resource {}", title);
            }
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
                Resource resource;
                if (magnet == null) {
                    resource = new Resource(uri, title);
                } else {
                    resource = new Resource(magnet, title, uri);
                }
                resourceRepository.save(resource);
                resources.add(resource);
                logger.debug("find new resource {}", title);
            }
        }
    }

    private String convertTorrent(String uri, String title) {
        String name = (id++ / 20) + ".torrent";
        File file = new File(downloadDir, name);
        try {
            file.createNewFile();
            HttpUtils.downloadFile(uri, file);
            String magnet = BtUtils.torrent2Magnet(file);
            logger.info("convert {} to {}", title, magnet);
            return magnet;
        } catch (Exception e) {
            logger.error("convert torrent to magnet failed: " + title, e);
            eventRepository.save(new Event(uri, "convert torrent to magnet failed: " + title));
        }
        return null;
    }

    private boolean isResource(String uri) {
        return uri.startsWith("magnet") || uri.startsWith("ed2k://") || uri.startsWith("thunder://")
            || uri.startsWith("ftp://") || uri.contains("pan.baidu.com");
    }

}
