package org.har01d.imovie.rs05;

import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.ConfigRepository;
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

    @Autowired
    private ConfigRepository configRepository;

    @Override
    public void crawler() throws InterruptedException {
        int total = 0;
        int page = getPage();
        while (true) {
            String url = baseUrl + page;
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("#movielist li");
                logger.info("get {} movies", elements.size());
                if (elements.isEmpty()) {
                    savePage(0);
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
                        eventRepository.save(new Event(pageUrl, "cannot get DouBan url"));
                        logger.warn("cannot get douban url for {}", pageUrl);
                        continue;
                    }

                    Movie movie = movieRepository.findFirstByDbUrl(dbUrl);
                    if (movie == null) {
                        try {
                            movie = douBanParser.parse(dbUrl);
                            movieRepository.save(movie);
                        } catch (Exception e) {
                            eventRepository.save(new Event(dbUrl, e.getMessage()));
                            logger.error("Parse page failed: " + title, e);
                        }
                    }

                    if (movie != null) {
                        try {
                            parser.parse(pageUrl, movie);
                            sourceRepository.save(new Source(pageUrl));
                            count++;
                            total++;
                        } catch (Exception e) {
                            eventRepository.save(new Event(pageUrl, e.getMessage()));
                            logger.error("Parse page failed: " + title, e);
                        }
                    }
                }

                if (count == 0) {
//                    break;
                }
            } catch (Exception e) {
                eventRepository.save(new Event(url, e.getMessage()));
                logger.error("Get HTML failed: " + url, e);
            }
            page++;
            savePage(page);
        }

        logger.info("=== get {} movies ===", total);
    }

    private int getPage() {
        String key = "rs05_page";
        Config config = configRepository.findOne(key);
        if (config == null) {
            return 0;
        }

        return Integer.valueOf(config.getValue());
    }

    private void savePage(int page) {
        configRepository.save(new Config("rs05_page", String.valueOf(page)));
    }

}
