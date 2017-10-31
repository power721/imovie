package org.har01d.imovie.sfz;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.har01d.imovie.AbstractCrawler;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Source;
import org.har01d.imovie.util.HttpUtils;
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
public class SfzCrawlerImpl extends AbstractCrawler implements SfzCrawler {

    private static final Logger logger = LoggerFactory.getLogger(SfzCrawler.class);

    @Value("${url.sfz}")
    private String baseUrl;

    @Autowired
    private SfzParser parser;

    private String[] types = {"", "tv", "cartoon"};

    @Override
    public void crawler() throws InterruptedException {
        Config crawler = getCrawlerConfig();
        if (!checkTime(crawler)) {
            return;
        }

        int page = getPage();
        int index = getConfig("index", 0);
        while (index < types.length) {
            String type = types[index];
            while (true) {
                handleError();
                String url = baseUrl + type + "/" + page;
                try {
                    String html = HttpUtils.getHtml(url);
                    Document doc = Jsoup.parse(html);
                    Elements elements = doc.select("ul.movie-list li");
                    logger.info("[SFZ-{}]{}-{}: get {} movies", index, page, total, elements.size());
                    if (elements.isEmpty()) {
                        break;
                    }

                    int count = 0;
                    for (Element element : elements) {
                        Element header = element.select("h2 a").first();
                        String title = header.text();
                        String pageUrl = header.attr("href");
                        if (title.contains("【通知】") || title.contains("[授之以渔]")) {
                            continue;
                        }
                        Source source = service.findSource(pageUrl);
                        if (source != null) {
                            if (source.isCompleted() || "".equals(type)) {
                                continue;
                            }

                            long time = System.currentTimeMillis();
                            if ((time - source.getUpdatedTime().getTime()) < TimeUnit.HOURS.toMillis(24)) {
                                logger.info("skip {}", pageUrl);
                                continue;
                            }
                        }

                        String dbUrl = UrlUtils.getDbUrl(element.select("div.des div p a[href*=douban]").attr("href"));
                        String imdbUrl = UrlUtils
                            .getImdbUrl(element.select("div.des div p a[href*=imdb]").attr("href"));

                        Movie movie = new Movie();
                        if (source != null && source.getMovieId() != null) {
                            movie.setId(source.getMovieId());
                        }
                        movie.setName(getName(title));
                        movie.setDbUrl(dbUrl);
                        movie.setImdbUrl(imdbUrl);
                        boolean completed = title.contains("【完结】");
                        try {
                            movie = parser.parse(pageUrl, movie);
                            if (movie != null) {
                                logger.info("[SFZ-{}] {}-{}-{} find movie {} {}", index, page, total, count,
                                    movie.getName(), pageUrl);
                                if (source == null) {
                                    source = new Source(pageUrl, getSourceTime(element));
                                }
                                source.setCompleted(completed);
                                source.setMovieId(movie.getId());
                                service.save(source);
                                count++;
                                total++;
                            } else {
                                if (source == null) {
                                    source = new Source(pageUrl, getSourceTime(element));
                                }
                                source.setCompleted(completed);
                                service.save(source);
                            }
                            error = 0;
                        } catch (Exception e) {
                            error++;
                            service.publishEvent(pageUrl, e.getMessage());
                            logger.error("Parse page failed: " + title, e);
                        }
                    }

                    if (crawler != null && count == 0) {
                        break;
                    }
                } catch (Exception e) {
                    error++;
                    service.publishEvent(url, e.getMessage());
                    logger.error("Get HTML failed: " + url, e);
                }
                page++;
                savePage(page);
            }
            page = 1;
            savePage(page);
            saveConfig("index", ++index);
        }

        saveCrawlerConfig();
        saveConfig("index", 0);
        savePage(1);
        logger.info("[SFZ]===== get {} movies =====", total);
    }

    private String getName(String title) {
        int index = title.indexOf('【');
        if (index > -1) {
            return title.substring(0, index).trim();
        }
        return title;
    }

    private Date getSourceTime(Element element) {
        String text = element.select("div.des div p span").first().ownText();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return df.parse(text);
        } catch (ParseException e) {
            logger.warn("get time failed.", e);
        }
        return new Date();
    }

}
