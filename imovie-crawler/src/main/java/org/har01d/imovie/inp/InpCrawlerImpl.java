package org.har01d.imovie.inp;

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
public class InpCrawlerImpl extends AbstractCrawler implements InpCrawler {

    private static final Logger logger = LoggerFactory.getLogger(InpCrawler.class);

    @Value("${url.inp.site}")
    private String siteUrl;

    @Value("${url.inp.page}")
    private String baseUrl;

    @Autowired
    private InpParser parser;

    private Random random = new Random();

    @Override
    public void crawler() throws InterruptedException {
        work(1);
        work(2);
        work(3);
        work(4);
    }

    private void work(int id) {
        int total = 0;
        int error = 0;
        int page = getPage(String.valueOf(id));
        Config crawler = getCrawlerConfig(String.valueOf(id));
        while (true) {
            String url = String.format(baseUrl, id, page);
            try {
                if (error >= 5) {
                    logger.warn("sleep {} seconds", error * 30L);
                    TimeUnit.SECONDS.sleep(error * 30L);
                }

                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.sortbox ul.relist li div.minfo a.title");
                if (elements.size() == 0) {
                    crawler = saveCrawlerConfig(String.valueOf(id));
                    page = 1;
                    continue;
                }
                logger.info("[inp-{}] {}: {} movies", id, page, elements.size());

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
                            logger.info("[inp-{}] {}-{}-{} find movie {}", id, page, total, count, movie.getName());
                            source = new Source(pageUrl, movie.getSourceTime());
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
                        logger.error("[inp-{}] Parse page failed: {}", id, pageUrl, e);
                    }
                }

                if (!doc.select("div.pages a").last().text().equals("尾页")) {
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
                logger.error("[inp-{}] Get HTML failed: {}", id, url, e);
            }
        }

        saveCrawlerConfig(String.valueOf(id));
        savePage(String.valueOf(id), 1);
        logger.info("[inp-{}] ===== get {} movies =====", id, total);
    }

    private String getName(Element element) {
        return element.text().replace("国语", "").replace("未删减版", "")
            .replace("真人版", "").replace("(未删减)", "")
            .replace("修复加长版", "").replace("(加长版)", "");
    }

}