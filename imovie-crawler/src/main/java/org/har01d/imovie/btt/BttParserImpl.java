package org.har01d.imovie.btt;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.bt.BtUtils;
import org.har01d.imovie.domain.Category;
import org.har01d.imovie.domain.Language;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Person;
import org.har01d.imovie.domain.Region;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.domain.Source;
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

    private Pattern CHINESE = Pattern.compile("[\\u4e00-\\u9fa5]");
    private Pattern DB_NAME = Pattern.compile("\\s*(\\S+)的剧情简介");
    private static final String[] TOKENS = new String[]{"导演:", "编剧:", "主演:", "类型:", "制片国家/地区:", "国家/地区:", "语言:", "对白:",
        "上映日期:", "日期:", "上映:", "上映时间:", "片长:", "又名:", "IMDb链接:", "官方网站:", "官网:", "压制:", "地区:", "字幕:",
        "首播:", "季数:", "集数:", "单集片长:", "资源发布网站:", "出品:", "发售日期:", "重播:", "来源:", "演员:", "译名:", "媒介:",
        "IMDB评分:", "简介:", "剧情简介", "影片介绍:", " 简 ", " 简  "};
    private static final String[] TOKENS2 = new String[]{"中文片名：", "导演：", "编剧：", "主演：", "类型：", "级别：", "发行：", "国家：",
        "片长：", "上映日期：", "字幕：", "年代：", "发布：", "播出时间：", "语言：", "官方网站："};
    private static final String[] TOKENS3 = new String[]{"年代：", "类    型：", "地区：", "地区：", "制作公司：", "语言：",
        "上映日期：", "英文：", "别名：", "编剧：", "导演：", "主演：", "简介："};

    @Value("${url.btt.site}")
    private String siteUrl;

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
        html = doc.select("div.post").html();
        String text = doc.select("div.post").text();

        Movie m = getMovie(html, text, movie);
        if (m == null) {
            m = searchByImdb(movie);
        }

        if (m == null) {
            m = searchByName(movie);
        }

        if (m == null) {
            logger.warn("Cannot find movie for {}: {}", movie.getName(), url);
            service.publishEvent(url, "Cannot find movie for " + movie.getName());
            return null;
        }

        Set<Resource> resources = m.getResources();
        int size = resources.size();
        Elements elements = doc.select("div.post p");
        findResource(elements.text(), resources);

        findResource(doc, resources);
        findAttachments(doc, resources);

        logger.info("get {}/{} resources for movie {}", (resources.size() - size), resources.size(), m.getName());
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
        if (imdb == null) {
            imdb = getImdbUrl(text);
        }
        if (imdb != null) {
            Movie m = service.findByImdb(imdb);
            if (m != null) {
                return m;
            }
            movie.setImdbUrl(imdb);
        }

        getMetadata(text, movie);
        if (movie.getName() == null) {
            movie.setName(fixName(getOne(movie.getAliases())));
        }
        if (movie.getName() == null) {
            getName(text, movie);
        }
        if (movie.getYear() == null) {
            movie.setYear(service.getYear(movie.getReleaseDate()));
        }
        return findMovie(movie);
    }

    private void getName(String text, Movie movie) {
        Matcher m = DB_NAME.matcher(text);
        if (m.find()) {
            movie.setName(fixName(m.group(1)));
        }
    }

    private String fixName(String name) {
        if (name == null) {
            return null;
        }

        name = name.replace("(台)", "").replace("(港)", "");
        if (name.endsWith(")")) {
            int len = name.length();
            int index = name.lastIndexOf('(');
            if (index > -1 && len - index >= 4) {
                return name.substring(0, index);
            }
        }
        return name;
    }

    private void getMetadata(String text, Movie movie) {
        if (text.contains("◎类　　别") || text.contains("◎片　　名") || text.contains("◎中 文 名")) {
            int start = text.indexOf("◎国　　家") + 6;
            int end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setRegions(getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎产　　地") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setRegions(getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎类　　别") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎类　　型") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎导　　演") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setDirectors(getPersons(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎语　　言") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎年　　代") + 5;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("◎上映日期") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("◎片　　长") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setRunningTime(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("◎译　　名") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }

            start = text.indexOf("◎片　　名") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }

            start = text.indexOf("◎中文片名") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }

            start = text.indexOf("◎中 文 名") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }

            start = text.indexOf("◎英文片名") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("◎片　 　名")) {
            int start = text.indexOf("◎国　 　家") + 6;
            int end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setRegions(getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎类　 　别") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎导　 　演") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setDirectors(getPersons(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎语　 　言") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎年　 　代") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("◎上映日 期") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("◎译　 　名") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }

            start = text.indexOf("◎片　 　名") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("◎译 名")) { // http://btbtt.co/thread-index-fid-1183-tid-4092435.htm
            int start = text.indexOf("◎国 家") + 4;
            int end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setRegions(getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎类 别") + 4;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎语 言") + 4;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎年 代") + 4;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("◎译 名") + 4;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }

            start = text.indexOf("◎片 名") + 4;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("◎译     名")) { // http://btbtt.co/thread-index-fid-1183-tid-4093225.htm
            int start = text.indexOf("◎ 国   家") + 7;
            int end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setRegions(getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎类     别") + 8;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎语     言") + 8;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎年     代") + 8;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("◎译     名") + 8;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }

            start = text.indexOf("◎片     名") + 8;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("◎译 　　名")) { // http://btbtt.co/thread-index-fid-1183-tid-4285832.htm
            int start = text.indexOf("◎国 　　家") + 6;
            int end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setRegions(getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎类 　　别") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎语 　　言") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎年 　　代") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("◎译 　　名") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }

            start = text.indexOf("◎片 　　名") + 6;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("◎中文片名")) { // http://btbtt.co/thread-index-fid-1183-tid-4354378.htm
            int start = text.indexOf("◎地　　区") + 5;
            int end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setRegions(getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎类　　型") + 5;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎语　　言") + 5;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎年　　代") + 5;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("◎中文片名") + 5;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }

            start = text.indexOf("◎英文片名") + 5;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("◎中    文    名：")) { // http://btbtt.co/thread-index-fid-1183-tid-4172428.htm
            int start = text.indexOf("◎类          型：") + 14;
            int end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎对 白 语 言：") + 9;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("◎年          代：") + 14;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("◎中    文    名：") + 13;
            end = text.indexOf("◎", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("@ 译.........名：")) { // http://btbtt.co/thread-index-fid-1183-tid-4139697.htm
            int start = text.indexOf("@ 类.........别：") + 14;
            int end = text.indexOf("@", start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("@ 语.........言：") + 14;
            end = text.indexOf("@", start);
            if (start > 20 && end > start) {
                movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("@ 发行时间：") + 7;
            end = text.indexOf("@", start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("@ 译.........名：") + 14;
            end = text.indexOf("@", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("【译 名】：")) { // http://btbtt.co/thread-index-fid-1183-tid-4172432.htm
            int start = text.indexOf("【国 家】：") + 6;
            int end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setRegions(getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【影片类型】：") + 7;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【语 言】：") + 6;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【上映时间】：") + 7;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("【译 名】：") + 6;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("【译名】：")) { // http://btbtt.co/thread-index-fid-1183-tid-4140337.htm
            int start = text.indexOf("【国家】：") + 5;
            int end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setRegions(getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【影片类型】：") + 7;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【语言】：") + 5;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【上映时间】：") + 7;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("【译名】：") + 5;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("【中文译名】")) { // http://btbtt.co/thread-index-fid-1183-tid-4172508.htm
            int start = text.indexOf("【国　　家】") + 6;
            int end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setRegions(getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【类　　别】") + 6;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【对白语言】") + 6;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【出品年代】") + 6;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("【中文译名】") + 6;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("【译  名】")) { // http://btbtt.co/thread-index-fid-1183-tid-4141657.htm
            int start = text.indexOf("【國  家】") + 6;
            int end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setRegions(getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【類  別】") + 6;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【语  言】") + 6;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【年  代】") + 6;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("【译  名】") + 6;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }

            start = text.indexOf("【片  名】") + 6;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("【译　　名】")) { // http://btbtt.co/thread-index-fid-1183-tid-4069795.htm
            int start = text.indexOf("【国　　家】") + 6;
            int end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setRegions(getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【类　　别】") + 6;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【语　　言】") + 6;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【年　　代】") + 6;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("【译　　名】") + 6;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }

            start = text.indexOf("【片　　名】") + 6;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("【中文片名】：")) { // http://btbtt.co/thread-index-fid-951-tid-4237007.htm
            int start = text.indexOf("【国家地区】：") + 7;
            int end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setRegions(getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【影片类型】：") + 7;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【对白语言】：") + 7;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("【出品年代】：") + 7;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("【中文片名】：") + 7;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }

            start = text.indexOf("【英文片名】：") + 7;
            end = text.indexOf("【", start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("中文片名：")) { // http://btbtt.co/thread-index-fid-951-tid-4236792.htm
            int start = text.indexOf("国家：") + 3;
            int end = getNextToken2(text, start);
            if (start > 20 && end > start) {
                movie.setRegions(getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("类型：") + 3;
            end = getNextToken2(text, start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

//            start = text.indexOf("【对白语言】") + 6;
//            end = text.indexOf("：", start);
//            if (start > 20 && end > start) {
//                movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
//            }

            start = text.indexOf("上映日期：") + 5;
            end = getNextToken2(text, start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("中文片名：") + 5;
            end = getNextToken2(text, start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else if (text.contains("别名：")) { // http://btbtt.co/thread-index-fid-1183-tid-4140915.htm
            int start = text.indexOf("地区：") + 3;
            int end = getNextToken3(text, start);
            if (start > 20 && end > start) {
                movie.setRegions(getRegions(getValues(text.substring(start, end))));
            }

            start = text.indexOf("类    型：") + 7;
            end = getNextToken3(text, start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues(text.substring(start, end))));
            }

            start = text.indexOf("语言：") + 3;
            end = getNextToken3(text, start);
            if (start > 20 && end > start) {
                movie.setLanguages(getLanguages(getValues(text.substring(start, end))));
            }

            start = text.indexOf("上映日期：") + 5;
            end = getNextToken3(text, start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("别名：") + 3;
            end = getNextToken3(text, start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }

            start = text.indexOf("英文：") + 3;
            end = getNextToken3(text, start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues(text.substring(start, end)));
            }
        } else {
            int start = text.indexOf("制片国家/地区:") + 8;
            int end = getNextToken(text, start);
            if (start > 20 && end > start) {
                movie.setRegions(getRegions(getValues2(text.substring(start, end))));
            }

            start = text.indexOf("类型:") + 3;
            end = getNextToken(text, start);
            if (start > 20 && end > start) {
                movie.setCategories(getCategories(getValues2(text.substring(start, end))));
            }

            start = text.indexOf("语言:") + 3;
            end = getNextToken(text, start);
            if (start > 20 && end > start) {
                movie.setLanguages(getLanguages(getValues2(text.substring(start, end))));
            }

            start = text.indexOf("上映日期:") + 5;
            end = getNextToken(text, start);
            if (start > 20 && end > start) {
                movie.setReleaseDate(getValue(text.substring(start, end), 120));
            }

            start = text.indexOf("片名:") + 3;
            end = getNextToken(text, start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues2(text.substring(start, end)));
            }

            start = text.indexOf("剧名:") + 3;
            end = getNextToken(text, start);
            if (start > 20 && end > start) {
                movie.getAliases().addAll(getValues2(text.substring(start, end)));
            }

            start = text.indexOf("又名:") + 3;
            end = getNextToken(text, start);
            if (start > 20 && end > start) {
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
            if (CHINESE.matcher(element).find()) {
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
        for (String token : TOKENS2) {
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
        for (String token : TOKENS) {
            int i = text.indexOf(token, start);
            if (i > 0 && (i < index || index == -1)) {
                index = i;
            }
        }
        return index;
    }

    private int getNextToken3(String text, int start) {
        int index = text.indexOf(" / ", start);
        for (String token : TOKENS3) {
            int i = text.indexOf(token, start);
            if (i > 0 && (i < index || index == -1)) {
                index = i;
            }
        }
        return index;
    }

    private String getValue(String text, int len) {
        text = text.replaceAll("　", "").replaceAll(" ", "").replaceAll("：", "").replaceAll(" ", "").replace("–", "")
            .replace("-", "")
            .trim();
        return StringUtils.truncate(text, len);
    }

    private Set<String> getValues(String text) {
        Set<String> values = new LinkedHashSet<>();
        String regex = "/";
        String[] vals = text.split(regex);
        for (String val : vals) {
            val = val.replaceAll("　", "").replaceAll(" ", "").replaceAll("：", "").replaceAll(" ", "").replace("–", "")
                .replace("-", "")
                .trim();
            if (!val.isEmpty()) {
                values.add(val);
            }
        }

        return values;
    }

    private Set<String> getValues2(String text) {
        Set<String> values = new LinkedHashSet<>();
        String regex = " / ";
        String[] vals = text.split(regex);
        for (String val : vals) {
            val = val.replaceAll(" ", "").trim();
            if (!val.isEmpty()) {
                values.add(val);
            }
        }

        return values;
    }

    private Movie findMovie(Movie movie) {
        String name = movie.getName();
        if (name == null) {
            return null;
        }

        List<Movie> movies = service.findByName(name);
//        for (String alias : movie.getAliases()) {
//            movies.addAll(service.findByName(alias));
//        }
        if (movies.isEmpty()) {
            name = getOne(movie.getAliases());
            if (name == null) {
                return null;
            }
            movies = service.findByName(name);
        }

        return findBestMatchedMovie(movies, movie);
    }

    private Movie findBestMatchedMovie(List<Movie> movies, Movie movie) {
        Movie best = null;
        int maxMatch = 0;
        for (Movie m : movies) {
            int match = 0;
            if (movie.getName().equals(m.getName())) {
                match += 2;
            } else if (m.getName().startsWith(movie.getName())) {
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

            if (movie.getDirectors() != null && !movie.getDirectors().isEmpty() && m.getDirectors() != null) {
                if (m.getDirectors().containsAll(movie.getDirectors())) {
                    match++;
                }
            }

            if (movie.getReleaseDate() != null && m.getReleaseDate() != null) {
                if (m.getReleaseDate().contains(movie.getReleaseDate())) {
                    match++;
                }
            }

            if (movie.getRunningTime() != null && m.getRunningTime() != null) {
                if (m.getRunningTime().equals(movie.getRunningTime())) {
                    match++;
                }
            }

            if (match > 2 && match > maxMatch) {
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
            String url = matcher.group(1).replace("http://", "https://");
            if (url.endsWith("/")) {
                return url;
            } else {
                return url + "/";
            }
        }
        return null;
    }

    private String getImdbUrl(String html) {
        int index = html.indexOf("imdb.com/title/");
        if (index > 0) {
            String text = html.substring(index - "https://www.".length(), index + 40);
            Matcher matcher = UrlUtils.IMDB.matcher(text);
            if (matcher.find()) {
                return "http://www.imdb.com/title/" + matcher.group(1);
            }
        }

        index = html.indexOf("www.imdb.cn/title/");
        if (index > 0) {
            String text = html.substring(index - "https://".length(), index + 40);
            Matcher matcher = UrlUtils.IMDB.matcher(text);
            if (matcher.find()) {
                return "http://www.imdb.com/title/" + matcher.group(1);
            }
        }

        index = html.indexOf("IMDb链接");
        if (index > 0) {
            String text = html.substring(index + 6, index + 24);
            Matcher matcher = UrlUtils.IMDB.matcher(text);
            if (matcher.find()) {
                return "http://www.imdb.com/title/" + matcher.group(1);
            }
        }

        index = html.indexOf("IMDB 链 接");
        if (index > 0) {
            String text = html.substring(index + 8, index + 25);
            Matcher matcher = UrlUtils.IMDB.matcher(text);
            if (matcher.find()) {
                return "http://www.imdb.com/title/" + matcher.group(1);
            }
        }

        index = html.indexOf("@ I..M..D..B：");
        if (index > 0) {
            String text = html.substring(index + 13, index + 28);
            Matcher matcher = UrlUtils.IMDB.matcher(text);
            if (matcher.find()) {
                return "http://www.imdb.com/title/" + matcher.group(1);
            }
        }

        return null;
    }

    private Movie searchByImdb(Movie movie) {
        if (movie.getImdbUrl() == null) {
            return null;
        }

        String imdb = movie.getImdbUrl().replace("http://www.imdb.com/title/", "");
        Movie m = searchMovie(movie, imdb);
        if (m != null) {
            return m;
        }

        logger.warn("[IMDB] cannot find movie for {}: {}", movie.getName(), imdb);
        service.publishEvent(imdb, "[IMDB] cannot find movie " + movie.getName());
        return null;
    }

    private Movie searchByName(Movie movie) {
        if (movie.getName() == null) {
            return null;
        }

        return searchMovie(movie, movie.getName());
    }

    private Movie searchMovie(Movie movie, String text) {
        String url;
        try {
            url = "https://movie.douban.com/subject_search?search_text=" + URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error("search movie failed: " + text, e);
            return null;
        }

        try {
            String html = HttpUtils.getHtml(url);
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("div.article a.nbg");
            if (elements.size() == 1) {
                String dbUrl = elements.attr("href");
                if (dbUrl.contains("movie.douban.com/subject/")) {
                    return douBanParser.parse(dbUrl);
                }
            } else {
                List<Movie> movies = new ArrayList<>();
                for (Element element : elements) {
                    String dbUrl = element.attr("href");
                    if (dbUrl.contains("movie.douban.com/subject/") && service.findByDbUrl(dbUrl) == null) {
                        try {
                            Movie m = douBanParser.parse(dbUrl);
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
                return findBestMatchedMovie(movies, movie);
            }
        } catch (Exception e) {
            service.publishEvent(url, e.getMessage());
            logger.error("Parse page failed: " + url, e);
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

        String name = (id.getAndIncrement() % 20) + ".torrent";
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

    private Set<Category> getCategories(Set<String> names) {
        Set<Category> categories = new HashSet<>();
        for (String name : names) {
            Category c = new Category(name);
            categories.add(c);
        }
        return categories;
    }

    private Set<Language> getLanguages(Set<String> names) {
        Set<Language> languages = new HashSet<>();
        for (String name : names) {
            Language l = new Language(name);
            languages.add(l);
        }
        return languages;
    }

    private Set<Region> getRegions(Set<String> names) {
        Set<Region> regions = new HashSet<>();
        for (String name : names) {
            Region r = new Region(name);
            regions.add(r);
        }
        return regions;
    }

    private Set<Person> getPersons(Set<String> names) {
        Set<Person> people = new HashSet<>();
        for (String name : names) {
            Person p = new Person(name);
            people.add(p);
        }
        return people;
    }

}
