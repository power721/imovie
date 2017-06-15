package org.har01d.imovie.rarbt;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.har01d.bittorrent.TorrentFile;
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
public class RarBtParserImpl implements RarBtParser {

    private static final Logger logger = LoggerFactory.getLogger(RarBtParser.class);

    @Value("${url.rarbt.site}")
    private String baseUrl;

    @Value("${file.download}")
    private File downloadDir;

    private AtomicInteger id = new AtomicInteger();

    @Autowired
    private DouBanParser douBanParser;

    @Autowired
    private MovieService service;

    @Override
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);

        Movie m = null;
        String dbUrl = movie.getDbUrl();
        if (dbUrl != null) {
            m = service.findByDbUrl(dbUrl);
            if (m == null) {
                m = douBanParser.parse(dbUrl);
            }
        }

        if (m == null) {
            String imdb = getImdbUrl(html);
            if (imdb != null) {
                m = service.findByImdb(imdb);
                movie.setImdbUrl(imdb);
            }
        }

        if (m != null) {
            Set<Resource> resources = m.getRes();
            int size = resources.size();
            resources.addAll(getResource(doc));

            logger.info("get {}/{} resources for movie {}", (resources.size() - size), resources.size(), m.getName());
            service.save(m);
            return m;
        } else {
            getResource(doc);
        }

        logger.warn("Cannot find movie for {}-{}: {}", movie.getName(), movie.getTitle(), url);
        service.publishEvent(url, "Cannot find movie for " + movie.getName() + " - " + movie.getTitle());
        return null;
    }

    private Set<Resource> getResource(Document doc) {
        Set<Resource> resources = new HashSet<>();
        Elements elements = doc.select(".sl .tinfo a");
        for (Element element : elements) {
            String uri = baseUrl + element.attr("href");
            String title = element.attr("title");
            if (!title.contains("论坛下载.") && !title.contains("百度网盘")) {
                title = title.replace("本地下载.", "").replace("[本地下载].", "").replace("【种子下载】.", "").trim();
                TorrentFile info = convertTorrent(uri, title);
                if (info != null) {
                    String fileSize = StringUtils.convertFileSize(info.getTotalLength());
                    String temp = fileSize;
                    if (temp.length() > 2) {
                        temp = fileSize.substring(fileSize.length() - 2, fileSize.length());
                        if (!title.contains(temp)) {
                            title = title + " " + fileSize;
                        }
                    }
                    String magnet = info.getMagnet();
                    logger.info("convert {} to {}", title, magnet);
                    resources.add(service.saveResource(magnet, uri, title));
                }
            }
        }
        return resources;
    }

    private String getImdbUrl(String html) {
        int index = html.indexOf("imdb:");
        if (index > 0) {
            String text = html.substring(index + 5, index + 24);
            Matcher matcher = UrlUtils.IMDB.matcher(text);
            if (matcher.find()) {
                return "http://www.imdb.com/title/" + matcher.group(1);
            }
        }

        return null;
    }

    private TorrentFile convertTorrent(String uri, String title) {
        String name = (id.getAndIncrement() % 20) + "-1.torrent";
        File file = new File(downloadDir, name);
        try {
            downloadDir.mkdirs();
            file.createNewFile();
            List<NameValuePair> params = URLEncodedUtils.parse(new URI(uri), "UTF-8");
            for (NameValuePair pair : params) {
                if ("zz".equals(pair.getName())) {
                    params.add(new BasicNameValuePair("zz", "zz" + pair.getValue()));
                    break;
                }
            }
            List<Header> headers = new ArrayList<>();
            String newUri = HttpUtils.downloadFile(uri, params, headers, file);
            if (newUri != null) {
                if (newUri.isEmpty()) {
                    throw new IOException("download file failed!");
                }
                if (!newUri.endsWith(".torrent")) {
                    return null;
                }
                newUri = new String(newUri.getBytes("ISO-8859-1"), "UTF-8");
                if (newUri.startsWith("/")) {
                    newUri = baseUrl + newUri;
                }
                logger.info("newUri: {}", newUri);
                int index = newUri.lastIndexOf('/');
                if (index > -1) {
                    newUri =
                        newUri.substring(0, index) + "/" + URLEncoder
                            .encode(newUri.substring(index + 1, newUri.length()), "UTF-8")
                            .replaceAll("\\+", "%20");
                }
                logger.info("newUri: {}", newUri);
                HttpUtils.downloadFile(newUri, file);
            }

            return new TorrentFile(file);
        } catch (Exception e) {
            logger.error("convert torrent to magnet failed: " + title, e);
            service.publishEvent(uri, "convert torrent to magnet failed: " + title);
        }
        return null;
    }

}
