package org.har01d.imovie.sfz;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.har01d.imovie.AbstractCrawler;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.Movie;
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
public class SfzCrawlerImpl extends AbstractCrawler implements SfzCrawler {

    private static final Logger logger = LoggerFactory.getLogger(SfzCrawler.class);

    @Value("${url.sfz}")
    private String baseUrl;

    @Autowired
    private SfzParser parser;

    @Override
    public void crawler() throws InterruptedException {
        Config crawler = getCrawlerConfig();
        if (!checkTime(crawler)) {
            return;
        }

        int page = getPage();
        while (true) {
            handleError();
            String url = baseUrl + page;
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("ul.movie-list li");
                logger.info("{}-{}: get {} movies", page, total, elements.size());
                if (elements.isEmpty()) {
                    if (crawler != null) {
                        break;
                    }
                    crawler = saveCrawlerConfig();
                    page = 1;
                    continue;
                }

                int count = 0;
                for (Element element : elements) {
                    Element header = element.select("h2 a").first();
                    String title = header.text();
                    String pageUrl = header.attr("href");
                    if (title.contains("【通知】") || title.contains("[授之以渔]")) {
                        continue;
                    }
                    if (service.findSource(pageUrl) != null) {
                        continue;
                    }

                    String dbUrl = element.select("div.des div p a[href*=douban]").attr("href");
                    String imdbUrl = element.select("div.des div p a[href*=imdb]").attr("href");

                    Movie movie = new Movie();
                    movie.setName(getName(title));
                    movie.setDbUrl(dbUrl);
                    movie.setImdbUrl(imdbUrl);
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger
                                .info("[SFZ] {}-{}-{} find movie {} {}", page, total, count, movie.getName(), pageUrl);
                            Source source = new Source(pageUrl, getSourceTime(element));
                            source.setMovieId(movie.getId());
                            service.save(source);
                            count++;
                            total++;
                        } else {
                            service.save(new Source(pageUrl, getSourceTime(element)));
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

        saveCrawlerConfig();
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
