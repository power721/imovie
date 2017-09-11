package org.har01d.imovie.dyb;

import java.util.Random;
import java.util.concurrent.TimeUnit;
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
public class DybCrawlerImpl extends AbstractCrawler implements DybCrawler {

    private static final Logger logger = LoggerFactory.getLogger(DybCrawler.class);

    @Value("${url.dyb.site}")
    private String siteUrl;

    @Value("${url.dyb.page}")
    private String baseUrl;

    @Autowired
    private DybParser parser;

    private Random random = new Random();

    @Override
    public void crawler() throws InterruptedException {
        work(1);
        work(2);
        work(3);
        work(4);
    }

    @Override
    public boolean isNew() {
        return getCrawlerConfig("1") == null
            && getCrawlerConfig("2") == null
            && getCrawlerConfig("3") == null
            && getCrawlerConfig("4") == null;
    }

    private void work(int id) throws InterruptedException {
        Config crawler = getCrawlerConfig(String.valueOf(id));
        if (!checkTime(crawler)) {
            return;
        }

        int page = getPage(String.valueOf(id));
        while (true) {
            handleError();
            String url = String.format(baseUrl, id, page);
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.list3_cn_box div.cn_box2 ul li.title h2 a");
                if (elements.size() == 0) {
                    if (crawler != null) {
                        break;
                    }
                    crawler = saveCrawlerConfig(String.valueOf(id));
                    page = 1;
                    continue;
                }
                logger.info("[dyb-{}] {}: {} movies", id, page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = siteUrl + element.attr("href");
                    Source source = service.findSource(pageUrl);
                    if (source != null) {
                        logger.info("skip {}", pageUrl);
                        continue;
                    }

                    Movie movie = new Movie();
                    movie.setName(getName(element));
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("[dyb-{}] {}-{}-{} find movie {}", id, page, total, count, movie.getName());
                            source = new Source(pageUrl, movie.getSourceTime(), movie.isCompleted());
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
                        logger.error("[dyb-{}] Parse page failed: {}", id, pageUrl, e);
                    }
                }

                if (!doc.select("div.page a").last().text().equals("尾页")) {
                    crawler = saveCrawlerConfig(String.valueOf(id));
                    page = 1;
                    continue;
                }

                if (crawler != null && count == 0) {
                    break;
                }
                page++;
                savePage(String.valueOf(id), page);
                TimeUnit.MILLISECONDS.sleep(random.nextInt(500));
            } catch (Exception e) {
                error++;
                service.publishEvent(url, e.getMessage());
                logger.error("[dyb-{}] Get HTML failed: {}", id, url, e);
            }
        }

        saveCrawlerConfig(String.valueOf(id));
        savePage(String.valueOf(id), 1);
        logger.info("[dyb-{}] ===== get {} movies =====", id, total);
    }

    private String getName(Element element) {
        return element.text().split("/")[0]
            .replace("电影版", "")
            .replace("HD", "")
            .replaceFirst("\\[.+]", "")
            .replaceFirst("\\(.+\\)", "")
            ;
    }

}
