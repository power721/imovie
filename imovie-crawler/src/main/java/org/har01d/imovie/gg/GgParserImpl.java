package org.har01d.imovie.gg;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.har01d.bittorrent.TorrentFile;
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
public class GgParserImpl extends AbstractParser implements GgParser {

    private static final Logger logger = LoggerFactory.getLogger(GgParser.class);

    @Value("${url.gg.site}")
    private String siteUrl;

    @Value("${url.gg.site}")
    private String baseUrl;

    @Value("${file.download}")
    private File downloadDir;

    private AtomicInteger id = new AtomicInteger();

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
            resources.addAll(findResource(doc, url));

            logger.info("[GaGa] get {}/{} resources for movie {} {}", (resources.size() - size), resources.size(),
                m.getName(), url);
            service.save(m);
            return m;
        } else {
            findResource(doc, url);
        }

        logger.warn("Cannot find movie for {}: {}", movie.getName(), url);
        service.publishEvent(url, "Cannot find movie for " + movie.getName());
        return null;
    }

    private Set<Resource> findResource(Document doc, String pageUrl) {
        Set<Resource> resources = new HashSet<>();
        Elements elements = doc.select("div.resource-res-list ul li.resource-res-li table.resource-res-table tr td");
        for (Element element : elements) {
            String uri = element.select("a").last().attr("href");
            String title = element.select("a").last().text();
            if (isResource(uri)) {
                try {
                    if (uri.startsWith("thunder://") || uri.startsWith("flashget://")) {
                        resources.add(service.saveResource(UrlUtils.convertUrl(uri), uri, title));
                    } else {
                        resources.add(service.saveResource(uri, title));
                    }
                } catch (Exception e) {
                    service.publishEvent(pageUrl, e.getMessage());
                    logger.error("[GaGa] get resource failed", e);
                }
            } else if (uri.contains("www.lwgod.com/forum.php?mod=attachment")) {
                TorrentFile info = convertTorrent(uri, title, title.contains(".torrent"));
                if (info != null) {
                    String magnet = info.getMagnet();
                    String fileSize = StringUtils.convertFileSize(info.getTotalLength());
                    if (!title.contains(fileSize)) {
                        title = title + " " + fileSize;
                    }
                    logger.info("convert {} to {}", title, magnet);
                    resources.add(service.saveResource(magnet, uri, title));
                }
            } else if (uri.contains("www.gagazz.com/torrent/show/") || uri.contains("kuai.xunlei.com/") || uri
                .contains("www.simplecd.me/download/") || uri.contains("urlxf.qq.com/") || uri.contains("Flashget://")
                || uri.contains("u.71kan.net/job.php") || uri.contains("dl.vmall.com/")) {
                // ignore
            } else if (!uri.trim().isEmpty()) {
                logger.warn("{} is not resource", uri);
            }
        }

        return resources;
    }

    protected boolean isResource(String uri) {
        return uri != null && (uri.startsWith("magnet:?")
            || uri.startsWith("ed2k://")
            || uri.startsWith("thunder://")
            || uri.startsWith("flashget://")
            || uri.startsWith("ftp://")
            || uri.contains("pan.baidu.com/")
            || uri.endsWith(".mp4")
            || uri.endsWith(".mkv")
            || uri.endsWith(".avi")
            || uri.endsWith(".rmvb")
            || uri.endsWith(".torrent")
        );
    }

    private TorrentFile convertTorrent(String uri, String title, boolean isTorrent) {
        if (!isTorrent) {
            return null;
        }

        String name = (id.getAndIncrement() % 20) + "-3.torrent";
        File file = new File(downloadDir, name);
        try {
            downloadDir.mkdirs();
            file.createNewFile();
            HttpUtils.downloadFile(uri, file);
            return new TorrentFile(file);
        } catch (Exception e) {
            logger.error("[GaGa] convert torrent to magnet failed: " + title, e);
            service.publishEvent(uri, "convert torrent to magnet failed: " + title);
        }
        return null;
    }

    private void getMovie(Document doc, Movie movie) {
        Elements elements = doc.select(
            "div#content .movie_large_dl .movie-show-detail .large-movie-detail .large-movie-detail-major ul li");
        for (Element element : elements) {
            getMetadata(movie, element);
        }
    }

    private void getMetadata(Movie movie, Element element) {
        String type = element.select("span").text();
        String text = element.ownText();
        if (type.contains("片名")) {
            text = element.select("a").text();
            movie.setName(getValue(text.replaceAll("\\(\\d{4}\\)", "")));
            movie.setTitle(getValue(text));
        } else if (type.contains("导演")) {
            movie.setDirectors(getPeople(getValues(text)));
        } else if (type.contains("作者")) {
            movie.setEditors(getPeople(getValues(text)));
        } else if (type.contains("主演")) {
            movie.setActors(getPeople(getValues(text)));
        } else if (type.contains("类别")) {
            movie.setCategories(getCategories(getValues(text)));
        } else if (type.contains("地区")) {
            movie.setRegions(getRegions(getValues(text)));
        } else if (type.contains("语言")) {
            movie.setLanguages(getLanguages(getValues(text)));
        } else if (type.contains("又名")) {
            movie.setAliases(getValues(text));
        } else if (type.contains("上映日期")) {
            movie.setReleaseDate(getValue(text));
        } else if (type.contains("首播")) {
            movie.setReleaseDate(getValue(text));
        } else if (type.contains("片长")) {
            movie.setRunningTime(getValue(text));
        } else if (type.contains("单集片长")) {
            movie.setRunningTime(getValue(text));
        } else if (type.contains("官方网站")) {
            movie.setWebsite(getValue(text));
        } else if (type.contains("集数")) {
            movie.setEpisode(Integer.valueOf(getValue(text)));
        } else if (type.contains("IMDb链接")) {
            movie.setImdbUrl(UrlUtils.getImdbUrl(getValue(text)));
        }
    }

    protected String getValue(String text) {
        return text.trim();
    }

    protected Set<String> getValues(String text) {
        Set<String> values = new HashSet<>();

        String regex = " / ";
        String[] vals = text.split(regex);
        if (vals.length == 1 && text.contains("/")) {
            vals = text.split("/");
        }

        for (String val : vals) {
            values.add(val.trim());
        }

        return values;
    }

}
