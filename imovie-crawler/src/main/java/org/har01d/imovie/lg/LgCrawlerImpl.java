package org.har01d.imovie.lg;

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
public class LgCrawlerImpl extends AbstractCrawler implements LgCrawler {

    private static final Logger logger = LoggerFactory.getLogger(LgCrawler.class);

    @Value("${url.lg}")
    private String baseUrl;

    @Autowired
    private LgParser parser;

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
                Elements elements = doc.select("div.mikd .mi_cont .mrb ul li h3.dytit a");
                if (elements.size() == 0) {
                    break;
                }
                logger.info("[lg] {}: {} movies", page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = element.attr("href");
                    Source source = service.findSource(pageUrl);
                    if (source != null) {
                        logger.info("skip {}", pageUrl);
                        continue;
                    }

                    Movie movie = new Movie();
                    movie.setName(element.text());
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("[lg] {}-{}-{} find movie {}", page, total, count, movie.getName());
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
                        logger.error("[lg] Parse page failed: " + pageUrl, e);
                    }
                }

                if (doc.select(".pagenavi_txt a").last().text().equals(String.valueOf(page))) {
                    crawler = saveCrawlerConfig();
                    page = 1;
                    break;
                }

                if (crawler != null && count == 0) {
                    break;
                }
                page++;
                savePage(page);
            } catch (Exception e) {
                error++;
                service.publishEvent(url, e.getMessage());
                logger.error("[lg] Get HTML failed: " + url, e);
            }
        }

        saveCrawlerConfig();
        savePage(1);
        logger.info("[lg] ===== get {} movies =====", total);
    }

}
