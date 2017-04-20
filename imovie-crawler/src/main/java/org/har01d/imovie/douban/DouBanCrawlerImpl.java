package org.har01d.imovie.douban;

import java.io.IOException;
import org.har01d.imovie.domain.Event;
import org.har01d.imovie.domain.EventRepository;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.MovieRepository;
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
public class DouBanCrawlerImpl implements DouBanCrawler {

    private static final Logger logger = LoggerFactory.getLogger(DouBanCrawler.class);

    @Value("${url.douban}")
    private String baseUrl;

    @Autowired
    private DouBanParser parser;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private EventRepository eventRepository;

    private String[] tags = new String[]{"爱情", "喜剧", "剧情", "动画", "科幻", "动作", "经典", "悬疑", "青春",
        "犯罪", "惊悚", "文艺", "搞笑", "纪录片", "励志", "恐怖", "战争", "黑色幽默", "魔幻", "传记",
        "情色", "暴力", "家庭", "音乐", "童年", "浪漫", "女性", "黑帮", "史诗", "童话", "西部",};

    @Override
    public void crawler() throws InterruptedException {
        int total = 0;
        for (String tag : tags) {
            int start = 0;
            while (true) {
                String url = String.format("%s/tag/%s?start=%d&type=T", baseUrl, tag, start);
                try {
                    String html = HttpUtils.getHtml(url);
                    Document doc = Jsoup.parse(html);
                    Elements elements = doc.select(".article a.nbg");
                    logger.info("get {} movies", elements.size());
                    if (elements.isEmpty()) {
                        break;
                    }

                    for (Element element : elements) {
                        String pageUrl = element.attr("href");
                        Movie movie = movieRepository.findFirstByDbUrl(pageUrl);
                        if (movie == null) {
                            try {
                                movie = parser.parse(pageUrl);
                                movieRepository.save(movie);
                                total++;
                            } catch (IOException e) {
                                eventRepository.save(new Event(pageUrl, e.getMessage()));
                                logger.error("Parse page failed: " + pageUrl, e);
                            }
                        }
                    }
                } catch (IOException e) {
                    eventRepository.save(new Event(url, e.getMessage()));
                    logger.error("Get HTML failed: " + url, e);
                }
                start += 20;
            }
        }

        logger.info("get {} movies", total);
    }
}
