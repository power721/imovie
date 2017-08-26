package org.har01d.imovie.ck;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
public class CkCrawlerImpl extends AbstractCrawler implements CkCrawler {

    private static final Logger logger = LoggerFactory.getLogger(CkCrawler.class);
    private static final Pattern NAME = Pattern.compile("(.+)\\d+-\\d+合集");

    @Value("${url.ck.movie}")
    private String baseUrl;

    @Autowired
    private CkParser parser;

    @Override
    public void crawler() throws InterruptedException {
        int total = 0;
        int error = 0;
        int page = getPage();
        Config crawler = getCrawlerConfig();
        while (true) {
            String url = baseUrl + page;
            try {
                if (error >= 5) {
                    if (error >= 10) {
                        return;
                    }
                    logger.warn("sleep {} seconds", error * 30L);
                    TimeUnit.SECONDS.sleep(error * 30L);
                }

                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc
                    .select("div.list-content .filtrate-container-body .fcb-inner ul li h3.p-meta-title a");
                if (elements.size() == 0) {
                    crawler = saveCrawlerConfig();
                    page = 1;
                    continue;
                }
                logger.info("[ck] {}: {} movies", page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = element.attr("href");
                    Source source = service.findSource(pageUrl);
                    if (source != null) {
                        logger.info("skip {}", pageUrl);
                        continue;
                    }

                    Movie movie = new Movie();
                    movie.setName(getName(element.text()));
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("[ck] {}-{}-{} find movie {}", page, total, count, movie.getName());
                            source = new Source(pageUrl, movie.getSourceTime());
                            source.setMovieId(movie.getId());
                            count++;
                            total++;
                        } else {
                            source = new Source(pageUrl, false);
                        }
                        service.save(source);
                        error = 0;
                    } catch (Exception e) {
                        error++;
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("[ck] Parse page failed: " + pageUrl, e);
                    }
                }

                if (doc.select(".wp-pagenavi").first().children().last().text().equals(String.valueOf(page))) {
                    crawler = saveCrawlerConfig();
                    page = 1;
                    continue;
                }

                if (crawler != null && count == 0) {
                    break;
                }
                page++;
                savePage(page);
            } catch (Exception e) {
                error++;
                service.publishEvent(url, e.getMessage());
                logger.error("[ck] Get HTML failed: " + url, e);
            }
        }

        saveCrawlerConfig();
        savePage(1);
        logger.info("[ck] ===== get {} movies =====", total);
    }

    private String getName(String text) {
        Matcher matcher = NAME.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return text;
    }

}
