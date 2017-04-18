package org.har01d.imovie.btt;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.MovieRepository;
import org.har01d.imovie.domain.SourceRepository;
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
        .compile("^\\[\\d+] \\[[^]]+] \\[[^]]+] \\[[^]]+]\\[([^]]+)]\\[[^]]+]\\[[^]]+]\\[[^]]+].*$");

    @Value("${url.btt.page}")
    private String baseUrl;

    @Value("${url.btt.site}")
    private String siteUrl;

    @Autowired
    private BttParser parser;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Override
    public void crawler() throws InterruptedException {
        int page = 1;
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
                        logger.info(matcher.group(1));
                        String pageUrl = siteUrl + element.select("a").first().attr("href");
                        logger.info(pageUrl);
                        if (sourceRepository.findFirstByUri(pageUrl) != null) {
                            continue;
                        }

                        Movie movie = new Movie();
                        movie.setTitle(text);
                        movie.setName(getName(text));
                        parser.parse(pageUrl, movie);
                        count++;
                    }
                }

                if (count == 0) {
//                    break;
                }
                page++;
            } catch (IOException e) {
                logger.error("Get HTML failed!", e);
            }

        }
    }


    private String getName(String title) {
        String[] comps = title.split("/");
        return comps[0];
    }

}
