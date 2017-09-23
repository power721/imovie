package org.har01d.imovie.ihd;

import java.util.ArrayList;
import java.util.List;
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
public class IhdCrawlerImpl extends AbstractCrawler implements IhdCrawler {

    private static final Logger logger = LoggerFactory.getLogger(IhdCrawler.class);

    @Value("${url.ihd}")
    private String baseUrl;

    @Autowired
    private IhdParser parser;

    @Override
    public void crawler() throws InterruptedException {
        Config crawler = getCrawlerConfig();
        if (!checkTime(crawler)) {
            return;
        }

        List<String> genres = getGenres();
        int index = getConfig("index", 0);
        while (index < genres.size()) {
            handleError();
            String url = genres.get(index);
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.col-xs-6 a");
                logger.info("[IHD-{}/{}]-{}", index + 1, genres.size(), elements.size());
                int count = 0;
                int skip = 0;
                for (Element element : elements) {
                    if (skip > 20 && crawler != null) {
                        break;
                    }

                    String pageUrl = baseUrl + element.attr("href");
                    Source source = service.findSource(pageUrl);
                    if (source != null) {
                        skip++;
                        logger.info("skip {}", pageUrl);
                        continue;
                    }
                    skip = 0;

                    Movie movie = new Movie();
                    movie.setName(element.select("div.caption").text());
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("[IHD-{}/{}] {}-{}/{} find movie {}", index + 1, genres.size(), total, count,
                                elements.size(), movie.getName());
                            source = new Source(pageUrl, true);
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
                        logger.error("[IHD] Parse page failed: " + pageUrl, e);
                    }
                }
            } catch (Exception e) {
                error++;
                service.publishEvent(url, e.getMessage());
                logger.error("[IHD] Parse page failed: " + url, e);
            }
            saveConfig("index", ++index);
        }

        saveCrawlerConfig();
        saveConfig("index", 0);
        logger.info("[IHD] ===== get {} movies =====", total);
    }

    private List<String> getGenres() {
        List<String> genres = new ArrayList<>();
        String url = baseUrl + "/inc/type_genres.html?index_id=" + random.nextFloat();
        try {
            String html = HttpUtils.getHtml(url);
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("select#typelist option");
            for (Element element : elements) {
                genres.add(baseUrl + element.attr("value") + ".shtml");
            }
        } catch (Exception e) {
            error++;
            service.publishEvent(url, e.getMessage());
            logger.error("[IHD] Get genres failed: {}", url, e);
        }

        return genres;
    }

}
