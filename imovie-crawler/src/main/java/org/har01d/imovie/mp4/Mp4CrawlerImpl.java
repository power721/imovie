package org.har01d.imovie.mp4;

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
public class Mp4CrawlerImpl extends AbstractCrawler implements Mp4Crawler {

    private static final Logger logger = LoggerFactory.getLogger(Mp4Crawler.class);

    @Value("${url.mp4.site}")
    private String siteUrl;

    @Value("${url.mp4.page}")
    private String baseUrl;

    @Autowired
    private Mp4Parser parser;

    @Override
    public void crawler() throws InterruptedException {
        Config crawler = getCrawlerConfig();
        if (!checkTime(crawler)) {
            return;
        }

        int page = getPage();
        while (true) {
            handleError();
            String url = baseUrl + page;
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("div.shannel ul li h2 a");
                if (elements.size() == 0) {
                    break;
                }
                logger.info("[mp4] {}: {} movies", page, elements.size());

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
                            logger.info("[mp4] {}-{}-{} find movie {}", page, total, count, movie.getName());
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
                        logger.error("[mp4] Parse page failed: " + pageUrl, e);
                    }
                }

                if (!doc.select("div.page a").last().text().equals("尾页")) {
                    crawler = saveCrawlerConfig();
                    page = 1;
                    continue;
                }

                if (crawler != null && count == 0) {
                    break;
                }
                page++;
                savePage(page);
            } catch (Exception e) {
                error++;
                service.publishEvent(url, e.getMessage());
                logger.error("[mp4] Get HTML failed: " + url, e);
            }
        }

        saveCrawlerConfig();
        savePage(1);
        logger.info("[mp4] ===== get {} movies =====", total);
    }

    private String getName(Element element) {
        return element.text().replace("国语", "").replace("未删减版", "")
            .replace("真人版", "").replace("(未删减)", "")
            .replace("修复加长版", "").replace("(加长版)", "");
    }

}
