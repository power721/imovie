package org.har01d.imovie.btt;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.AbstractCrawler;
import org.har01d.imovie.domain.Category;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Region;
import org.har01d.imovie.domain.Source;
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
public class BttCrawlerImpl extends AbstractCrawler implements BttCrawler {

    private static final Logger logger = LoggerFactory.getLogger(BttCrawler.class);
    private static final Pattern SUBJECT_PATTERN = Pattern
        .compile("^\\[([^]]+)] \\[[^]]+] \\[([^]]+)] \\[([^]]+)]\\[([^]]+)]\\[[^]]+]\\[[^]]+].*$");

    private static final Pattern SUBJECT_PATTERN2 = Pattern
        .compile("^\\[([^]]+)] \\[[^]]+] \\[([^]]+)] (?:\\[连载] )?(?:\\[打包] )?(?:\\[全集] )?(?:\\[合集] )?\\[[^]]+](.*)$");

    private static final Pattern SUBJECT_PATTERN3 = Pattern
        .compile("^\\[([^]]+)] \\[([^]]+)] \\[([^]]+)] \\[[^]]+] \\[[^]]+](.*) [0-9.]+[GM]B$");

    private static final Pattern SUBJECT_PATTERN4 = Pattern
        .compile("^\\[([^]]+)] \\[(\\d+)]\\[([^]]+)]\\[[^]]+]\\[([^]]+)].*$");

    private static final Pattern SUBJECT_PATTERN5 = Pattern
        .compile("\\[([^]]+)] \\[([^]]+)] \\[[^]]+] \\[([^]]+)] 【.+?】 【.+?】【.+?】【(.+?)】.*$");

    private static final Pattern NUMBER = Pattern.compile("(\\d+)");

    @Value("${url.btt.page}")
    private String baseUrl;

    @Value("${url.btt.site}")
    private String siteUrl;

    @Value("${crawler.type:all}")
    private Set<String> types;

    @Autowired
    private BttParser parser;

    @Override
    public void crawler() throws InterruptedException {
        if (types.contains("all") || types.contains("951")) {
            work(951);
        }

        if (types.contains("all") || types.contains("1183")) {
            work(1183);
        }

        if (types.contains("all") || types.contains("950")) {
            work(950);
        }

        if (types.contains("all") || types.contains("981")) {
            work(981);
        }

        if (types.contains("all") || types.contains("1193")) {
            work(1193);
        }
    }

    @Override
    public boolean isNew() {
        return getCrawlerConfig("951") == null && getCrawlerConfig("1183") == null
            && getCrawlerConfig("950") == null
            && getCrawlerConfig("981") == null && getCrawlerConfig("1193") == null;
    }

    private void work(int fid) {
        Config crawler = getCrawlerConfig(String.valueOf(fid));
        if (!checkTime(crawler)) {
            return;
        }

        int zero = 0;
        int error = 0;
        int total = 0;
        int page = getPage(String.valueOf(fid));
        while (true) {
            String url = String.format(baseUrl, fid, page);
            try {
                if (error >= 5) {
                    logger.warn("{}: sleep {} seconds", fid, error * 30L);
                    TimeUnit.SECONDS.sleep(error * 30L);
                }

                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("td.subject");
                int last = getNumber(doc.select("div.page a.checked").text());
                if (page > last || elements.size() == 0) {
                    logger.info("get last page");
                    crawler = saveCrawlerConfig(String.valueOf(fid));
                    page = 1;
                    error = 0;
                    continue;
                }

                int count = 0;
                for (Element element : elements) {
                    String text = element.text();
                    if (text.contains("【站务】") || text.contains("[公告]") || text.contains("版规") || text.contains("每日更新 汇总")) {
                        continue;
                    }
                    Movie movie = new Movie();
                    String pageUrl = siteUrl + element.select("a.subject_link").attr("href");
                    if (service.findSource(fixPageUrl(pageUrl)) != null) {
                        logger.info("skip {}", pageUrl);
                        continue;
                    }
                    movie.setTitle(element.select("a.subject_link").text());
                    String name = getName(movie.getTitle());
                    movie.setName(name);
                    boolean matched = false;
                    Matcher matcher = SUBJECT_PATTERN.matcher(text);
                    if (matcher.find()) {
                        String str = matcher.group(3).trim();
                        name = str;
                        if (str.contains("BT") || str.contains("下载") || str.contains("网盘")) {
                            name = matcher.group(4).trim();
                        }
                        name = getName(name);
                        logger.info("{}-{}-{}-{} {}: {} - {}", fid, page, total, count, name, pageUrl, 1);

                        String y = matcher.group(1);
                        movie.setName(name);
                        if (y.matches("\\d{4}")) {
                            movie.setYear(Integer.valueOf(y));
                        }
                        movie.setCategories(getCategories(Collections.singleton(matcher.group(2))));
                        matched = true;
                    }

                    if (!matched) {
                        matcher = SUBJECT_PATTERN3.matcher(text);
                        if (matcher.find()) {
                            name = getName(matcher.group(4).trim());
                            logger.info("{}-{}-{}-{} {}: {} - {}", fid, page, total, count, name, pageUrl, 3);

                            String y = matcher.group(1);
                            movie.setName(name);
                            if (y.matches("\\d{4}")) {
                                movie.setYear(Integer.valueOf(y));
                            }
                            movie.setCategories(getCategories(Collections.singleton(matcher.group(3))));
                            movie.setRegions(getRegions(Collections.singleton(matcher.group(2))));
                            matched = true;
                        }
                    }

                    if (!matched) {
                        matcher = SUBJECT_PATTERN5.matcher(text);
                        if (matcher.find()) {
                            name = getName(matcher.group(3).trim());
                            logger.info("{}-{}-{}-{} {}: {} - {}", fid, page, total, count, name, pageUrl, 2);

                            String y = matcher.group(1);
                            movie.setName(name);
                            if (y.matches("\\d{4}")) {
                                movie.setYear(Integer.valueOf(y));
                            }
                            movie.setRegions(getRegions(Collections.singleton(matcher.group(2))));
                            matched = true;
                        }
                    }

                    if (!matched) {
                        matcher = SUBJECT_PATTERN2.matcher(text);
                        if (matcher.find()) {
                            name = getName(matcher.group(3).trim());
                            logger.info("{}-{}-{}-{} {}: {} - {}", fid, page, total, count, name, pageUrl, 2);

                            String y = matcher.group(1);
                            movie.setName(name);
                            if (y.matches("\\d{4}")) {
                                movie.setYear(Integer.valueOf(y));
                            }
                            movie.setCategories(getCategories(Collections.singleton(matcher.group(2))));
                            matched = true;
                        }
                    }

                    if (!matched) {
                        matcher = SUBJECT_PATTERN4.matcher(text);
                        if (matcher.find()) {
                            name = getName(matcher.group(4).trim());
                            logger.info("{}-{}-{}-{} {}: {} - {}", fid, page, total, count, name, pageUrl, 4);

                            String y = matcher.group(2);
                            movie.setName(name);
                            movie.setYear(Integer.valueOf(y));
                            movie.setCategories(getCategories(Collections.singleton(matcher.group(1))));
                            movie.setRegions(getRegions(Collections.singleton(matcher.group(3))));
                            matched = true;
                        }
                    }

                    if (!matched) {
                        if (fid == 981) {
                            zero++;
                            logger.warn("skip {}: {}", text, pageUrl);
                            continue;
                        }
                        logger.info("{}-{}-{}-{} {}: {} - {}", fid, page, total, count, name, pageUrl, 0);
                    }

                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            Source source = new Source(fixPageUrl(pageUrl), movie.getSourceTime());
                            source.setMovieId(movie.getId());
                            service.save(source);
                            if (movie.getSize() == 0) {
                                zero++;
                            } else {
                                zero = 0;
                            }
                            count++;
                            total++;
                        } else {
                            zero++;
                            service.save(new Source(fixPageUrl(pageUrl), false));
                        }
                        error = 0;
                    } catch (Exception e) {
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("Parse page failed: " + pageUrl, e);
                        error++;
                    }
                }

                if (crawler != null && count == 0) {
                    break;
                }
                if (zero > 60) {
                    if (crawler == null) {
                        crawler = saveCrawlerConfig(String.valueOf(fid));
                    }
                    logger.warn("too many empty resource, stopping...");
                    page = 1;
                    error = 0;
                    continue;
                }
                page++;
                savePage(String.valueOf(fid), page);
            } catch (Exception e) {
                service.publishEvent(url, e.getMessage());
                logger.error("Get HTML failed: " + url, e);
                error++;
            }
        }

        saveCrawlerConfig(String.valueOf(fid));
        savePage(String.valueOf(fid), 1);
        logger.info("===== {}: get {} movies =====", fid, total);
    }

    private String fixPageUrl(String url) {
        return url.replace(".pw/", ".me/").replace(".net/", ".me/").replace(".top/", ".me/").replace(".co/", ".me/");
    }

    private Integer getNumber(String text) {
        Matcher matcher = NUMBER.matcher(text);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }
        return Integer.MAX_VALUE;
    }

    private String getName(String title) {
        if (title.startsWith("[")) {
            int index = title.indexOf(']');
            if (index < 1) {
                index = title.length();
            }
            String temp = title.substring(1, index);
            temp = fixName(title, index, temp);
            index = title.indexOf(temp) + temp.length();
            temp = fixName(title, index, temp);
            index = title.indexOf(temp) + temp.length();
            temp = fixName(title, index, temp);
            title = temp.replace("未分级版", "").replace("未删减版", "").replace("鼠绘汉化", "").trim();
        }

        String[] comps = title.split("/");
        return comps[0];
    }

    private String fixName(String title, int index, String temp) {
        if (temp.contains("BT") || temp.contains("电驴") || temp.contains("下载") || temp.contains("网盘")
            || temp.contains("迅雷") || temp.contains("快传") || temp.contains("百度")
            || temp.contains("字幕组") || temp.contains("字幕組") || temp.contains("高清")
            || temp.contains("动漫") || temp.contains("動漫")
            || temp.contains("CONAN") || temp.contains("Skytree") || temp.contains("漫画")
            || temp.contains("三立") || temp.contains("民视") || temp.contains("中视") || temp.contains("台视")
            || temp.contains("TVB") || temp.contains("ATV") || temp.contains("HKTV") || temp.contains("Viu TV")) {
            int start = index + 1;
            index = title.indexOf(']', start);
            if (index < 0) {
                index = title.length();
            }
            if (start < 0 || start >= index) {
                return temp;
            }
            temp = title.substring(start, index);
            if (temp.startsWith("[")) {
                temp = temp.substring(1);
            }
        }
        return temp;
    }

    protected Set<Category> getCategories(Set<String> names) {
        Set<Category> categories = new HashSet<>();
        for (String name : names) {
            if (name.isEmpty()) {
                continue;
            }
            Category c = new Category(name);
            categories.add(c);
        }
        return categories;
    }

    protected Set<Region> getRegions(Set<String> names) {
        Set<Region> regions = new HashSet<>();
        for (String name : names) {
            if (name.isEmpty()) {
                continue;
            }
            if ("大陆".equals(name)) {
                name = "中国大陆";
            }
            Region r = new Region(name);
            regions.add(r);
        }
        return regions;
    }

}
