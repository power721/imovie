package org.har01d.imovie.rarbt;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
public class RarBtCrawlerImpl extends AbstractCrawler implements RarBtCrawler {

    private static final Logger logger = LoggerFactory.getLogger(RarBtCrawler.class);

    @Value("${url.rarbt.site}")
    private String siteUrl;

    @Value("${url.rarbt.page}")
    private String baseUrl;

    @Autowired
    private RarBtParser parser;

    @Override
    public void crawler() throws InterruptedException {
        int page = getPage();
        Config crawler = getCrawlerConfig();
        while (true) {
            handleError();
            String url = String.format(baseUrl, page);
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.ml .item .title");
                if (elements.size() == 0) {
                    if (crawler != null) {
                        break;
                    }
                    crawler = saveCrawlerConfig();
                    page = 1;
                    continue;
                }
                logger.info("{}: {} movies", page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = siteUrl + element.select(".tt a").attr("href");
                    if (service.findSource(pageUrl) != null) {
                        continue;
                    }

                    String db = siteUrl + element.select(".rt a").attr("href");
                    String dbUrl = getDbUrl(db);
                    Movie movie = new Movie();
                    movie.setDbUrl(dbUrl);
                    movie.setTitle(element.select(".tt a").text());

                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("{}-{}-{} find movie {}", page, total, count, movie.getName());
                            movie.setSourceTime(getSourceTime(element.select(".tt span").text()));
                            service.save(new Source(pageUrl, movie.getSourceTime()));
                            count++;
                            total++;
                        } else {
                            service.save(new Source(pageUrl, getSourceTime(element.select(".tt span").text()), false));
                        }
                        error = 0;
                    } catch (Exception e) {
                        error++;
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("Parse page failed: " + pageUrl, e);
                    }
                }

                if (crawler != null && count == 0) {
                    break;
                }
                page++;
                savePage(page);
            } catch (Exception e) {
                error++;
                service.publishEvent(url, e.getMessage());
                logger.error("Get HTML failed: " + url, e);
            }
        }

        saveCrawlerConfig();
        savePage(1);
        logger.info("[Rar]===== get {} movies =====", total);
    }

    private String getDbUrl(String redirectUrl) throws IOException {
        String html = HttpUtils.getHtml(redirectUrl);
        return UrlUtils.getDbUrl(html);
    }

    private Date getSourceTime(String text) {
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        try {
            return df.parse(text);
        } catch (ParseException e) {
            logger.warn("get time failed.", e);
        }
        return new Date();
    }

}
