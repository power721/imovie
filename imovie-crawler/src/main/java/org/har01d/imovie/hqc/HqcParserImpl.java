package org.har01d.imovie.hqc;

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
import org.har01d.imovie.util.TextUtils;
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
public class HqcParserImpl extends AbstractParser implements HqcParser {

    private static final Logger logger = LoggerFactory.getLogger(HqcParser.class);

    @Value("${url.hqc.site}")
    private String siteUrl;

    @Value("${url.hqc.site}")
    private String baseUrl;

    @Value("${file.download}")
    private File downloadDir;

    private AtomicInteger id = new AtomicInteger();

    @Override
    @Transactional
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);
        Movie m;
        if (movie.getId() != null) {
            m = service.findById(movie.getId());
        } else {
            String dbUrl = UrlUtils.getDbUrl(html);
            if (dbUrl != null) {
                m = getByDb(dbUrl);
            } else {
                getMovie(doc, movie);
                m = searchByName(movie);
            }
        }

        String text = doc.select("dl.row dd h3").text();
        movie.setCompleted(text.matches(".*全\\d+集.*"));
        if (m != null) {
            Set<Resource> resources = m.getRes();
            int size = resources.size();
            m.addResources(findResource(doc, url));

            logger.info("[HQC] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
            service.save(m);
            m.setNewResources(resources.size() - size);
            m.setCompleted(movie.isCompleted());
            m.setSourceTime(movie.getSourceTime());
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
        if (skipResource) {
            return resources;
        }

        Elements elements = doc.select("div.message ul.attachlist li a");
        for (Element element : elements) {
            String uri = siteUrl + element.attr("href");
            if (uri.contains("attach-download-") && service.findResource(uri) == null) {
                String title = element.text();
                try {
                    String magnet = null;
                    TorrentFile info = convertTorrent(uri, title, title.contains(".torrent"));
                    if (info != null) {
                        magnet = info.getMagnet();
                        String fileSize = TextUtils.convertFileSize(info.getTotalLength());
                        if (!title.contains(fileSize)) {
                            title = title + " " + fileSize;
                        }
                        logger.info("convert {} to {}", title, magnet);
                    }
                    resources.add(service.saveResource(magnet, uri, title));
                } catch (Exception e) {
                    service.publishEvent(pageUrl, e.getMessage());
                    logger.error("[HQC] get resource failed", e);
                }
            } else if (!uri.trim().isEmpty()) {
                logger.warn("{} is not resource", uri);
            }
        }

        return resources;
    }

    private TorrentFile convertTorrent(String uri, String title, boolean isTorrent) {
        if (!isTorrent) {
            return null;
        }

        String name = (id.getAndIncrement() % 20) + "-2.torrent";
        File file = new File(downloadDir, name);
        try {
            downloadDir.mkdirs();
            file.createNewFile();
            HttpUtils.downloadFile(uri, file);
            return new TorrentFile(file);
        } catch (Exception e) {
            logger.error("[HQC] convert torrent to magnet failed: " + title, e);
            service.publishEvent(uri, "convert torrent to magnet failed: " + title);
        }
        return null;
    }

    private void getMovie(Document doc, Movie movie) {
        String title = doc.select("dl.row dd h3").first().ownText();
        int index1 = title.indexOf('[');
        int index2 = title.indexOf(']');
        if (index1 > -1 && index2 > index1) {
            title = title.substring(index1 + 1, index2);
        }
        movie.setName(title);

        Elements elements = doc.select("div.message p");
        for (Element element : elements) {
            String text = element.text();
            if (text.contains("导演:") && text.contains("类型:") && text.contains("语言:")) {
                for (String line : convertElement2Lines(element)) {
                    getMetadata(movie, line);
                }
                return;
            } else {
                getMetadata(movie, text);
            }
        }
    }

    private void getMetadata(Movie movie, String text) {
        if (text.contains("导演:")) {
            movie.setDirectors(getPeople(getValues(text, "导演:")));
        } else if (text.contains("编剧:")) {
            movie.setEditors(getPeople(getValues(text, "编剧:")));
        } else if (text.contains("主演:")) {
            movie.setActors(getPeople(getValues(text, "主演:")));
        } else if (text.contains("类型:")) {
            movie.setCategories(getCategories(getValues(text, "类型:")));
        } else if (text.contains("制片国家/地区:")) {
            movie.setRegions(getRegions(getValues(text, "制片国家/地区:")));
        } else if (text.contains("语言:")) {
            movie.setLanguages(getLanguages(getValues(text, "语言:")));
        } else if (text.contains("又名:")) {
            movie.setAliases(getValues(text, "又名:"));
        } else if (text.contains("上映日期:")) {
            movie.setReleaseDate(getValue(text, "上映日期:"));
        } else if (text.contains("首播:")) {
            movie.setReleaseDate(getValue(text, "首播:"));
        } else if (text.contains("片长:")) {
            movie.setRunningTime(getValue(text, "片长:"));
        } else if (text.contains("单集片长:")) {
            movie.setRunningTime(getValue(text, "单集片长:"));
        } else if (text.contains("官方网站:")) {
            movie.setWebsite(getValue(text, "官方网站:"));
        } else if (text.contains("集数:")) {
            movie.setEpisode(Integer.valueOf(getValue(text, "集数:")));
        } else if (text.contains("IMDb链接:")) {
            movie.setImdbUrl(UrlUtils.getImdbUrl(getValue(text, "IMDb链接:")));
        } else if (text.contains("◎导　　演")) {
            movie.setDirectors(service.getPersons(getValues(text, "◎导　　演　")));
        } else if (text.contains("◎类　　别")) {
            movie.setCategories(service.getCategories(getValues(text, "◎类　　别　")));
        } else if (text.contains("◎国　　家")) {
            movie.setRegions(service.getRegions(getValues(text, "◎国　　家　")));
        } else if (text.contains("◎语　　言")) {
            movie.setLanguages(service.getLanguages(getValues(text, "◎语　　言　")));
        } else if (text.contains("◎译　　名")) {
            movie.setAliases(getValues(text, "◎译　　名　"));
        } else if (text.contains("◎上映日期")) {
            movie.setReleaseDate(getValue(text, "◎上映日期　"));
        } else if (text.contains("◎片　　长")) {
            movie.setRunningTime(getValue(text, "◎片　　长　"));
        } else if (text.contains("◎集　　数")) {
            movie.setEpisode(Integer.valueOf(getValue(text, "◎集　　数　")));
        }
    }

}
