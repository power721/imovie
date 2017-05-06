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

    private Pattern CHINEASE = Pattern.compile("[\\u4e00-\\u9fa5]");
    private static final String[] TOKENS = new String[]{"导演:", "编剧:", "主演:", "类型:", "制片国家/地区:", "语言:", "上映日期:",
        "片长:", "又名:", "IMDb链接:", "官方网站:", "首播:", "季数:", "集数:", "单集片长:", "剧情简介"};
    private static final String[] TOKENS2 = new String[]{"中文片名：", "导演：", "编剧：", "主演：", "类型：", "级别："
        , "发行：", "国家：", "片长：", "上映日期：", "官方网站："};

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
        html = doc.select("div.post").html();
        String text = doc.select("div.post").text();

        Movie m = getMovie(html, text, movie);
        if (m == null) {
            logger.warn("Cannot find movie for " + movie.getName());
            service.publishEvent(url, "Cannot find movie for " + movie.getName());
            return null;
        }

        Set<Resource> resources = m.getResources();
        Elements elements = doc.select("div.post p");
        for (Element element : elements) {
            findResource(element.text(), resources);
        }

        findResource(doc, resources);
        findAttachments(doc, resources);

        logger.info("get {} resources for movie {}", resources.size(), m.getName());
        service.save(m);
        return m;
    }

    private Movie getMovie(String html, String text, Movie movie) throws IOException {
        String dbUrl = getDbUrl(html);
        if (dbUrl != null) {
            Movie m = service.findByDbUrl(dbUrl);
            if (m == null) {
                m = douBanParser.parse(dbUrl);
            }
            return m;
        }

        String imdb = getImdbUrl(html);
        if (imdb != null) {
            Movie m = service.findByImdb(imdb);
            if (m != null) {
                return m;
            }
            movie.setImdbUrl(imdb);
        }

        getMetadata(text, movie);
        if (movie.getName() == null) {
            movie.setName(getOne(movie.getAliases()));
        }
        if (movie.getYear() == null) {
            service.getYear(movie, movie.getReleaseDate());
        }
        return findMovie(movie);
    }

    private void getMetadata(String text, Movie movie) {
        if (text.contains("◎类　　别") || text.contains("◎片　　名")) {
            int start = text.indexOf("◎国　　家") + 6;
            int end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setRegions(service.getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎类　　别") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setCategories(service.getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎语　　言") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setLanguages(service.getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎年　　代") + 5;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("◎上映日期") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("◎译　　名") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setAliases(getValues(text.substring(start, end)));
            }

            start = text.indexOf("◎片　　名") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }

            start = text.indexOf("◎中文片名") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }

            start = text.indexOf("◎英文片名") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("◎片　 　名")) {
            int start = text.indexOf("◎国　 　家") + 6;
            int end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setRegions(service.getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎类　 　别") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setCategories(service.getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎语　 　言") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setLanguages(service.getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎年　 　代") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("◎上映日 期") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("◎译　 　名") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setAliases(getValues(text.substring(start, end)));
            }

            start = text.indexOf("◎片　 　名") + 6;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("◎中    文    名：")) { // http://btbtt.co/thread-index-fid-1183-tid-4172428.htm
            int start = text.indexOf("◎类          型：") + 14;
            int end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setCategories(service.getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎对 白 语 言：") + 9;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setLanguages(service.getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎年          代：") + 14;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("◎中    文    名：") + 13;
            end = text.indexOf("◎", start);
            if (start > 10 && end > start) {
                movie.setAliases(getValues(text.substring(start, end)));
            }
        } else if (text.contains("【译 名】：")) { // http://btbtt.co/thread-index-fid-1183-tid-4172432.htm
            int start = text.indexOf("【国 家】：") + 6;
            int end = text.indexOf("【", start);
            if (start > 10 && end > start) {
                movie.setRegions(service.getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【影片类型】：") + 7;
            end = text.indexOf("【", start);
            if (start > 10 && end > start) {
                movie.setCategories(service.getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【语 言】：") + 6;
            end = text.indexOf("【", start);
            if (start > 10 && end > start) {
                movie.setLanguages(service.getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【上映时间】：") + 7;
            end = text.indexOf("【", start);
            if (start > 10 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("【译 名】：") + 6;
            end = text.indexOf("【", start);
            if (start > 10 && end > start) {
                movie.setAliases(getValues(text.substring(start, end)));
            }
        } else if (text.contains("【中文译名】")) { // http://btbtt.co/thread-index-fid-1183-tid-4172508.htm
            int start = text.indexOf("【国　　家】") + 6;
            int end = text.indexOf("【", start);
            if (start > 10 && end > start) {
                movie.setRegions(service.getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【类　　别】") + 6;
            end = text.indexOf("【", start);
            if (start > 10 && end > start) {
                movie.setCategories(service.getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【对白语言】") + 6;
            end = text.indexOf("【", start);
            if (start > 10 && end > start) {
                movie.setLanguages(service.getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【出品年代】") + 6;
            end = text.indexOf("【", start);
            if (start > 10 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("【中文译名】") + 6;
            end = text.indexOf("【", start);
            if (start > 10 && end > start) {
                movie.setAliases(getValues(text.substring(start, end)));
            }
        } else if (text.contains("【中文片名】：")) { // http://btbtt.co/thread-index-fid-951-tid-4237007.htm
            int start = text.indexOf("【国家地区】：") + 7;
            int end = text.indexOf("【", start);
            if (start > 10 && end > start) {
                movie.setRegions(service.getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【影片类型】：") + 7;
            end = text.indexOf("【", start);
            if (start > 10 && end > start) {
                movie.setCategories(service.getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【对白语言】：") + 7;
            end = text.indexOf("【", start);
            if (start > 10 && end > start) {
                movie.setLanguages(service.getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【出品年代】：") + 7;
            end = text.indexOf("【", start);
            if (start > 10 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("【中文片名】：") + 7;
            end = text.indexOf("【", start);
            if (start > 10 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }

            start = text.indexOf("【英文片名】：") + 7;
            end = text.indexOf("【", start);
            if (start > 10 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("中文片名：")) { // http://btbtt.co/thread-index-fid-951-tid-4236792.htm
            int start = text.indexOf("国家：") + 3;
            int end = getNextToken2(text, start);
            if (start > 10 && end > start) {
                movie.setRegions(service.getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("类型：") + 3;
            end = getNextToken2(text, start);
            if (start > 10 && end > start) {
                movie.setCategories(service.getCategories(getValues(text.substring(start, end))));
            }

//            start = text.indexOf("【对白语言】") + 6;
//            end = text.indexOf("：", start);
//            if (start > 10 && end > start) {
//                movie.setLanguages(service.getLanguages(getValues(text.substring(start, end))));
//            }

            start = text.indexOf("上映日期：") + 5;
            end = getNextToken2(text, start);
            if (start > 10 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("中文片名：") + 5;
            end = getNextToken2(text, start);
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
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("片名:") + 3;
            end = getNextToken(text, start);
            if (start > 10 && end > start) {
                movie.getAliases().addAll(getValues2(text.substring(start, end)));
            }

            start = text.indexOf("又名:") + 3;
            end = getNextToken(text, start);
            if (start > 10 && end > start) {
                movie.getAliases().addAll(getValues2(text.substring(start, end)));
            }
        }
    }

    private String getOne(Set<String> set) {
        if (set == null) {
            return null;
        }

        String result = null;
        for (String element : set) {
            if (result == null) {
                result = element;
            }
            if (CHINEASE.matcher(element).find()) {
                return element;
            }
        }
        return result;
    }

    private int getNextToken(String text, int start) {
        int index = text.indexOf(" / ", start);
        for (String token : TOKENS) {
            int i = text.indexOf(token, start);
            if (i > 0 && (i < index || index == -1)) {
                index = i;
            }
        }
        return index;
    }

    private int getNextToken2(String text, int start) {
        int index = text.indexOf("/", start);
        for (String token : TOKENS2) {
            int i = text.indexOf(token, start);
            if (i > 0 && (i < index || index == -1)) {
                index = i;
            }
        }
        return index;
    }

    private String getValue(String text, int len) {
        text = text.replaceAll("　", "").replaceAll(" ", "").replaceAll("  ", "").replace("–", "").replace("-", "")
            .trim();
        return StringUtils.truncate(text, len);
    }

    private Set<String> getValues(String text) {
        Set<String> values = new HashSet<>();
        String regex = "/";
        String[] vals = text.split(regex);
        for (String val : vals) {
            values.add(
                val.replaceAll("　", "").replaceAll(" ", "").replaceAll("  ", "").replace("–", "").replace("-", "")
                    .trim());
        }

        return values;
    }

    private Set<String> getValues2(String text) {
        Set<String> values = new HashSet<>();
        String regex = " / ";
        String[] vals = text.split(regex);
        for (String val : vals) {
            values.add(val.replaceAll(" ", "").trim());
        }

        return values;
    }

    private Movie findMovie(Movie movie) {
        String name = movie.getName();
        if (name == null) {
            return null;
        }

        List<Movie> movies = service.findByName(name);
        for (String alias : movie.getAliases()) {
            movies.addAll(service.findByName(alias));
        }
        Movie best = null;
        int maxMatch = 0;
        for (Movie m : movies) {
            int match = 0;
            if (name.equals(m.getName())) {
                match++;
            }

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
        Matcher matcher = UrlUtils.DB_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replace("http://", "https://");
        }
        return null;
    }

    private String getImdbUrl(String html) {
        int index = html.indexOf("www.imdb.com/title/");
        if (index > 0) {
            String text = html.substring(index - "https://".length(), index + 40);
            Matcher matcher = UrlUtils.IMDB_PATTERN.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        index = html.indexOf("IMDb链接");
        if (index > 0) {
            String text = html.substring(index + 6, index + 20);
            Matcher matcher = UrlUtils.IMDB.matcher(text);
            if (matcher.find()) {
                return "http://www.imdb.com/title/" + matcher.group(1);
            }
        }

        index = html.indexOf("IMDB 链 接");
        if (index > 0) {
            String text = html.substring(index + 8, index + 23);
            Matcher matcher = UrlUtils.IMDB.matcher(text);
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
                if (title.length() > 200) {
                    int index = title.indexOf(element.text());
                    if (index > 0) {
                        title = title.substring(index);
                    }
                }
                resources.add(service.saveResource(uri, StringUtils.truncate(title, 100)));
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
        if (!title.endsWith(".torrent")) {
            return null;
        }

        String name = (id++ % 20) + ".torrent";
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
