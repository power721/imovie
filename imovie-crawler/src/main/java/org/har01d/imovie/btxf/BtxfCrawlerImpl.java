package org.har01d.imovie.btxf;

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
public class BtxfCrawlerImpl extends AbstractCrawler implements BtxfCrawler {

    private static final Logger logger = LoggerFactory.getLogger(BtxfCrawler.class);

    @Value("${url.btxf.site}")
    private String siteUrl;

    @Value("${url.btxf.page}")
    private String baseUrl;

    @Autowired
    private BtxfParser parser;

    @Override
    public void crawler() throws InterruptedException {
        if (!checkTime()) {
            return;
        }

        int page = getPage();
        Config crawler = getCrawlerConfig();
        while (true) {
            handleError();
            String url = String.format(baseUrl, page);
            if (page == 1) {
                url = url.replace("index-1", "index");
            }

            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);

                String last = doc.select("div#pages a").last().text();
                if (!"尾页".equals(last)) {
                    crawler = saveCrawlerConfig();
                    page = 1;
                    continue;
                }

                Elements elements = doc.select("div.wrap .right ul.list li a.name");
                logger.info("[btxf] {}: {} movies", page, elements.size());

                int count = 0;
                for (Element element : elements) {
                    String pageUrl = siteUrl + element.attr("href");
                    if (service.findSource(pageUrl) != null) {
                        continue;
                    }

                    Movie movie = new Movie();
                    movie.setName(element.text());
                    try {
                        movie = parser.parse(pageUrl, movie);
                        if (movie != null) {
                            logger.info("[btxf] {}-{}-{} find movie {}", page, total, count, movie.getName());
                            service.save(new Source(pageUrl, movie.getSourceTime()));
                            count++;
                            total++;
                        }/* else {
                            service.save(new Source(pageUrl, false));
                        }*/
                        error = 0;
                    } catch (Exception e) {
                        error++;
                        service.publishEvent(pageUrl, e.getMessage());
                        logger.error("[btxf] Parse page failed: " + pageUrl, e);
                    }
                }

                if (crawler != null && count == 0) {
                    break;
                }
                page++;
                savePage(page);
            } catch (Exception e) {
                error++;
                service.publishEvent(url, e.getMessage());
                logger.error("[btxf] Get HTML failed: " + url, e);
            }
        }

        saveCrawlerConfig();
        savePage(1);
        logger.info("[btxf] ===== get {} movies =====", total);
    }

}
