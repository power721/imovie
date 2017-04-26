package org.har01d.imovie.btt;

import java.io.IOException;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Source;
import org.har01d.imovie.service.MovieService;
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
public class BttCrawlerImpl implements BttCrawler {

    private static final Logger logger = LoggerFactory.getLogger(BttCrawler.class);
    private static final Pattern SUBJECT_PATTERN = Pattern
        .compile("^\\[([^]]+)] \\[[^]]+] \\[([^]]+)] \\[[^]]+]\\[([^]]+)]\\[[^]]+]\\[[^]]+]\\[[^]]+].*$");

    @Value("${url.btt.page}")
    private String baseUrl;

    @Value("${url.btt.site}")
    private String siteUrl;

    @Autowired
    private BttParser parser;

    @Autowired
    private MovieService service;

    @Override
    public void crawler() throws InterruptedException {
        int total = 0;
        int page = getPage();
        while (page < 1000) {
            String url = String.format(baseUrl, page);
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("td.subject");

                int count = 0;
                for (Element element : elements) {
                    String text = element.text();
                    Matcher matcher = SUBJECT_PATTERN.matcher(text);
                    if (matcher.find()) {
                        String pageUrl = siteUrl + element.select("a").attr("href");
                        logger.info(page + "-" + count + " " + matcher.group(3) + ": " + pageUrl);
                        if (service.findSource(pageUrl) != null) {
                            continue;
                        }

                        String y = matcher.group(1);
                        Movie movie = new Movie();
                        movie.setTitle(text);
                        movie.setName(getName(matcher.group(3)));
                        if (y.matches("\\d{4}")) {
                            movie.setYear(Integer.valueOf(y));
                        }
                        movie.setCategories(service.getCategories(Collections.singleton(matcher.group(2))));

                        try {
                            movie = parser.parse(pageUrl, movie);
                            if (movie != null) {
                                service.save(new Source(pageUrl));
                                count++;
                                total++;
                            }
                        } catch (Exception e) {
                            service.publishEvent(pageUrl, e.getMessage());
                            logger.error("Parse page failed: " + pageUrl, e);
                        }
                    }
                }

                if (count == 0) {
//                    break;
                }
                page++;
                savePage(page);
            } catch (IOException e) {
                service.publishEvent(url, e.getMessage());
                logger.error("Get HTML failed: " + url, e);
            }
        }

        savePage(1);
        logger.info("===== get {} movies =====", total);
    }

    private String getName(String title) {
        String[] comps = title.split("/");
        return comps[0];
    }

    private int getPage() {
        String key = "btt_page";
        Config config = service.getConfig(key);
        if (config == null) {
            return 1;
        }

        return Integer.valueOf(config.getValue());
    }

    private void savePage(int page) {
        service.saveConfig("btt_page", String.valueOf(page));
    }

}
