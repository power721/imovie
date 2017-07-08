package org.har01d.imovie.douban;

import org.apache.http.impl.client.BasicCookieStore;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.service.MovieService;
import org.har01d.imovie.util.HttpUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Deprecated
@Service
public class DouBanCrawlerImpl implements DouBanCrawler {

    private static final Logger logger = LoggerFactory.getLogger(DouBanCrawler.class);
    private static final int LIMIT = 20;

    @Value("${url.douban}")
    private String baseUrl;

    @Autowired
    private DouBanParser parser;

    @Autowired
    private MovieService service;

    @Autowired
    private BasicCookieStore cookieStore;

    private static final String[] tags = new String[]{"爱情", "喜剧", "剧情", "动画", "科幻", "动作", "经典", "悬疑", "青春",
        "犯罪", "惊悚", "文艺", "搞笑", "纪录片", "励志", "恐怖", "战争", "黑色幽默", "魔幻", "传记", "情色", "暴力", "家庭",
        "音乐", "童年", "浪漫", "女性", "黑帮", "史诗", "童话", "西部", "电视剧", "人性", "奇幻"};

    @Override
    public void crawler() throws InterruptedException {
        int total = 0;
        JSONParser jsonParser = new JSONParser();
        for (int i = getTagIndex(); i < tags.length; ++i) {
            saveTagIndex(i);
            String tag = tags[i];
            int start = getStart();
            while (true) {
                String url = String.format("%s/j/search_subjects?tag=%s&sort=time&page_limit=%d&page_start=%d",
                    baseUrl, tag, LIMIT, start);
                try {
                    String json = HttpUtils.getJson(url, cookieStore);
                    JSONObject jsonObject = (JSONObject) jsonParser.parse(json);
                    JSONArray items = (JSONArray) jsonObject.get("subjects");

                    if (items == null || items.isEmpty()) {
                        break;
                    }
                    logger.info("({}/{}){}:{} get {} movies", i, tags.length - 1, tag, start, items.size());

                    int count = 0;
                    for (Object item1 : items) {
                        JSONObject item = (JSONObject) item1;
                        String pageUrl = (String) item.get("url");
                        Movie movie = service.findByDbUrl(pageUrl);
                        if (movie == null) {
                            try {
                                movie = parser.parse(pageUrl);
                                service.save(movie);
                                count++;
                                total++;
                            } catch (Exception e) {
                                service.publishEvent(pageUrl, e.getMessage());
                                logger.error("Parse page failed: " + pageUrl, e);
                            }
                        }
                    }

                    if (count == 0) {
                        break;
                    }
                } catch (Exception e) {
                    service.publishEvent(url, e.getMessage());
                    logger.error("Get HTML failed: " + url, e);
                }
                start += LIMIT;
                saveStart(start);
            }
            saveStart(0);
        }
        saveTagIndex(0);

        logger.info("===== get {} movies =====", total);
    }

    private int getTagIndex() {
        String key = "db_tag_index";
        Config config = service.getConfig(key);
        if (config == null) {
            return 0;
        }

        int index = Integer.valueOf(config.getValue());
        if (index >= tags.length) {
            index = 0;
        }
        return index;
    }

    private void saveTagIndex(int index) {
        service.saveConfig("db_tag_index", String.valueOf(index));
    }

    private int getStart() {
        String key = "db_tag_start";
        Config config = service.getConfig(key);
        if (config == null) {
            return 0;
        }

        return Integer.valueOf(config.getValue());
    }

    private void saveStart(int start) {
        service.saveConfig("db_tag_start", String.valueOf(start));
    }
}
