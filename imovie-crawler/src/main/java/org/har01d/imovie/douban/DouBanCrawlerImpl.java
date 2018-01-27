package org.har01d.imovie.douban;

import org.apache.http.impl.client.BasicCookieStore;
import org.har01d.imovie.AbstractCrawler;
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
import org.springframework.transaction.annotation.Transactional;

@Service
public class DouBanCrawlerImpl extends AbstractCrawler implements DouBanCrawler {

    private static final Logger logger = LoggerFactory.getLogger(DouBanCrawler.class);
    private static final int LIMIT = 50;

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

    private static final String[] tyTags = new String[]{"热门", "美剧", "英剧", "韩剧", "日剧", "国产剧", "港剧", "日本动画", "综艺", "纪录片"};
    private static final int[] types = new int[]{11, 24, 5, 13, 17, 25, 10, 19, 20, 16, 15, 12, 29, 30, 3, 22, 14, 7,
        28, 6, 26, 1,};

    @Override
    public void crawler() throws InterruptedException {
//        work0();
//        work1();
        work2();
    }

    private void work0() {
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
                    logger.info("({}/{}){}:{} get {} movies", i + 1, tags.length, tag, start, items.size());

                    int count = 0;
                    for (Object item1 : items) {
                        JSONObject item = (JSONObject) item1;
                        String pageUrl = (String) item.get("url");
                        try {
                            Movie movie = parser.parse(pageUrl, false);
                            service.updateMovie(pageUrl, movie);
                            count++;
                            total++;
                        } catch (Exception e) {
                            service.publishEvent(pageUrl, e.getMessage());
                            logger.error("Parse page failed: " + pageUrl, e);
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

    private void work1() {
        total = 0;
        for (int i = getTypeIndex(); i < types.length; ++i) {
            saveTypeIndex(i);
            int type = types[i];
            int start = getTypeStart();
            JSONParser jsonParser = new JSONParser();
            while (true) {
                String url =
                    "https://movie.douban.com/j/chart/top_list?type=" + type + "&interval_id=100%3A90&action=&start="
                        + start + "&limit=50";
                try {
                    String json = HttpUtils.getJson(url, cookieStore);
                    JSONArray items = (JSONArray) jsonParser.parse(json);
                    if (items == null || items.isEmpty()) {
                        break;
                    }
                    logger.info("({}/{}){}-{} get {}-{} movies", i + 1, types.length, type, start, total, items.size());

                    int count = 0;
                    for (Object item : items) {
                        updateDbScore((JSONObject) item);
                        count++;
                    }

                    if (count == 0) {
                        break;
                    }
                } catch (Exception e) {
                    service.publishEvent(url, e.getMessage());
                    logger.error("Get HTML failed: " + url, e);
                }
                start += 50;
                saveTypeStart(start);
            }
            saveTypeStart(0);
        }
        saveTypeIndex(0);

        logger.info("===== update {} movies =====", total);
    }

    private void work2() {
        JSONParser jsonParser = new JSONParser();
        for (int i = getTvTagIndex(); i < tyTags.length; ++i) {
            saveTvTagIndex(i);
            String tag = tyTags[i];
            int start = getTvStart();
            while (true) {
                String url = String.format("%s/j/search_subjects?type=tv&tag=%s&sort=time&page_limit=%d&page_start=%d",
                    baseUrl, tag, LIMIT, start);
                try {
                    String json = HttpUtils.getJson(url);
                    JSONObject jsonObject = (JSONObject) jsonParser.parse(json);
                    JSONArray items = (JSONArray) jsonObject.get("subjects");

                    if (items == null || items.isEmpty()) {
                        break;
                    }
                    logger.info("({}/{}){}:{} get {} movies", i + 1, tyTags.length - 1, tag, start, items.size());

                    int count = 0;
                    for (Object item : items) {
                        updateDbRate((JSONObject) item);
                        count++;
                    }

                    if (count == 0) {
                        break;
                    }
                } catch (Exception e) {
                    service.publishEvent(url, e.getMessage());
                    logger.error("Get HTML failed: " + url, e);
                }
                start += LIMIT;
                saveTvStart(start);
            }
            saveTvStart(0);
        }
        saveTvTagIndex(0);

        logger.info("===== get {} movies =====", total);
    }

    @Transactional
    @Override
    public void updateDbScore(JSONObject item) {
        String pageUrl = (String) item.get("url");
        String score = (String) item.get("score");
        Long vote = (Long) item.get("vote_count");
        try {
            Movie movie = service.findByDbUrl(pageUrl);
            if (movie == null) {
                return;
            }
            movie.setVotes(vote.intValue());
            movie.setDbScore(score);
            service.updateMovie(movie);
            total++;
        } catch (Exception e) {
            service.publishEvent(pageUrl, e.getMessage());
            logger.error("Parse page failed: " + pageUrl, e);
        }
    }

    @Transactional
    @Override
    public void updateDbRate(JSONObject item) {
        String pageUrl = (String) item.get("url");
        String score = (String) item.get("rate");
        try {
            Movie movie = service.findByDbUrl(pageUrl);
            if (movie == null) {
                return;
            }
            movie.setDbScore(score);
            service.updateMovie(movie);
            total++;
        } catch (Exception e) {
            service.publishEvent(pageUrl, e.getMessage());
            logger.error("Parse page failed: " + pageUrl, e);
        }
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

    private int getTypeIndex() {
        String key = "db_type_index";
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

    private void saveTypeIndex(int index) {
        service.saveConfig("db_type_index", String.valueOf(index));
    }

    private int getTypeStart() {
        String key = "db_type_start";
        Config config = service.getConfig(key);
        if (config == null) {
            return 0;
        }

        return Integer.valueOf(config.getValue());
    }

    private void saveTypeStart(int start) {
        service.saveConfig("db_type_start", String.valueOf(start));
    }


    private int getTvTagIndex() {
        String key = "db_tv_index";
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

    private void saveTvTagIndex(int index) {
        service.saveConfig("db_tv_index", String.valueOf(index));
    }

    private int getTvStart() {
        String key = "db_tv_start";
        Config config = service.getConfig(key);
        if (config == null) {
            return 0;
        }

        return Integer.valueOf(config.getValue());
    }

    private void saveTvStart(int start) {
        service.saveConfig("db_tv_start", String.valueOf(start));
    }

}
