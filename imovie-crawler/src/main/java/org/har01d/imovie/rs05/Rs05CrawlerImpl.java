package org.har01d.imovie.rs05;

import java.io.IOException;
import org.har01d.imovie.domain.Event;
import org.har01d.imovie.domain.EventRepository;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.MovieRepository;
import org.har01d.imovie.domain.Source;
import org.har01d.imovie.domain.SourceRepository;
import org.har01d.imovie.douban.DouBanParser;
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
public class Rs05CrawlerImpl implements Rs05Crawler {

    private static final Logger logger = LoggerFactory.getLogger(Rs05Crawler.class);

    @Value("${url.rs05}")
    private String baseUrl;

    @Autowired
    private Rs05Parser parser;

    @Autowired
    private DouBanParser douBanParser;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private EventRepository eventRepository;

    @Override
    public void crawler() throws InterruptedException {
        int total = 0;
        int page = 1319;
        while (true) {
            String url = baseUrl + page;
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("#movielist li");
                logger.info("get {} movies", elements.size());
                if (elements.isEmpty()) {
                    break;
                }

                int count = 0;
                for (Element element : elements) {
                    Element header = element.select(".intro h2 a").first();
                    String title = header.attr("title");
                    String pageUrl = header.attr("href");
                    if (sourceRepository.findFirstByUri(pageUrl) != null) {
                        continue;
                    }

                    String dbUrl = element.select(".intro .dou a").attr("href");

                    if (dbUrl.isEmpty()) {
                        logger.warn("cannot get douban url for {}", pageUrl);
                        continue;
                    }

                    try {
                        Movie movie = movieRepository.findFirstByDbUrl(dbUrl);
                        if (movie == null) {
                            movie = douBanParser.parse(dbUrl);
                            movieRepository.save(movie);
                        }

                        parser.parse(pageUrl, movie);
                        sourceRepository.save(new Source(pageUrl));
                        count++;
                    } catch (IOException e) {
                        if (eventRepository.findFirstBySource(pageUrl) == null) {
                            eventRepository.save(new Event(pageUrl, e.getMessage()));
                        }
                        logger.error("Parse page failed: " + title, e);
                    }
                }

                if (count == 0) {
//                    break;
                }
                total += count;
            } catch (IOException e) {
                logger.error("Get HTML failed: " + url, e);
            }
            page++;
        }

        logger.info("get {} movies", total);
    }

}
