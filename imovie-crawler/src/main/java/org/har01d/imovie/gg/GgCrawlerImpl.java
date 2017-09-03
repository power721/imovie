package org.har01d.imovie.gg;

import java.time.LocalDate;
import org.apache.http.client.HttpResponseException;
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
public class GgCrawlerImpl extends AbstractCrawler implements GgCrawler {

    private static final Logger logger = LoggerFactory.getLogger(GgCrawler.class);
    private final int size = 12;

    @Value("${url.gg.site}")
    private String siteUrl;

    @Value("${url.gg.page}")
    private String baseUrl;

    @Autowired
    private GgParser parser;

    private String[] nations = {"捷克", "奥地利", "瑞士", "匈牙利", "俄罗斯", "芬兰", "西德", "比利时", "荷兰", "波兰", "巴西", "丹麦", "苏联", "墨西哥",
        "阿根廷", "澳大利亚", "瑞典", "韩国", "印度", "西班牙", "加拿大", "德国", "意大利", "法国", "日本", "英国", "美国", "台湾", "香港", "中国大陆", ""};

    @Override
    public void crawler() throws InterruptedException {
        Config crawler = getCrawlerConfig();
        if (!checkTime(crawler)) {
            return;
        }

        if (crawler != null) {
            work();
            return;
        }

        int current = LocalDate.now().getYear();
        int index = getConfig("index", 0);
        while (index < nations.length) {
            int year1 = getConfig("year", 1880);
            while (year1 <= current) {
                int year2 = year1;
                if (year1 < 1970) {
                    year2 = year1 + 10;
                }

                int offset = getConfig("offset", 0);
                logger.info("[GaGa][{}/{}] {}: {}-{} {}", index + 1, nations.length, nations[index], year1, year2,
                    offset);
                while (offset < 1000) {
                    String url = baseUrl + String
                        .format("?category=&nation=%s&years=%d..%d&offset=%d", nations[index], year1, year2, offset);
                    if (work(offset, url)) {
                        break;
                    }
                    offset += size;
                    saveConfig("offset", offset);
                }

                if (year1 < 1970) {
                    year1 += 10;
                } else {
                    year1++;
                }
                saveConfig("year", year1);
                saveConfig("offset", 0);
            }
            saveConfig("year", 1880);
            saveConfig("index", ++index);
            saveConfig("offset", 0);
        }

        saveCrawlerConfig();
        saveConfig("offset", 0);
        deleteConfig("index");
        deleteConfig("year");
        logger.info("[GaGa] ===== get {} movies =====", total);
    }

    private void work() throws InterruptedException {
        int offset = getConfig("offset", 0);
        while (offset < 1000) {
            String url = baseUrl + "?category=&nation=&years=&offset=" + offset;
            if (work(offset, url)) {
                break;
            }
            offset += size;
            saveConfig("offset", offset);
        }
        saveConfig("offset", 0);
        logger.info("[GaGa] ===== get {} movies =====", total);
    }

    private boolean work(int offset, String url) throws InterruptedException {
        handleError();

        try {
            String html = HttpUtils.getHtml(url);
            Document doc = Jsoup.parse(html);
            error = 0;
            Elements elements = doc.select("li div.movie-res-detail-label div.movie-res-detail-img p a");
            if (elements.size() == 0) {
                saveCrawlerConfig();
                savePage(0);
                return true;
            }
            logger.info("[GaGa] {}: {} movies", offset, elements.size());

            int count = 0;
            for (Element element : elements) {
                String pageUrl = siteUrl + element.attr("href");
                Source source = service.findSource(pageUrl);
                if (source != null) {
                    continue;
                }

                Movie movie = new Movie();
                try {
                    movie = parser.parse(pageUrl, movie);
                    if (movie != null) {
                        logger
                            .info("[GaGa] {}-{}-{} find movie {} {}", offset, total, count, movie.getName(),
                                pageUrl);
                        source = new Source(pageUrl, movie.isCompleted());
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
                    logger.error("[GaGa] Parse page failed: {}", pageUrl, e);
                }
            }

            if (elements.size() < size) {
                return true;
            }
            sleep();
        } catch (Exception e) {
            if (e instanceof HttpResponseException && ((HttpResponseException) e).getStatusCode() == 404) {
                return true;
            }
            error++;
            service.publishEvent(url, e.getMessage());
            logger.error("[GaGa] Get HTML failed: {}", url, e);
        }
        return false;
    }

}
