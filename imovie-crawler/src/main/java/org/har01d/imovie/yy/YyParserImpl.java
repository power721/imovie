package org.har01d.imovie.yy;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.har01d.imovie.AbstractParser;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.StringUtils;
import org.har01d.imovie.util.UrlUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class YyParserImpl extends AbstractParser implements YyParser {

    private static final Logger logger = LoggerFactory.getLogger(YyParser.class);
    @Value("${url.yy}")
    private String baseUrl;

    @Override
    @Transactional
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);
        Movie m = null;
        if (movie.getId() != null) {
            m = service.findById(movie.getId());
        } else {
            String dbUrl = UrlUtils.getDbUrl(html);
            if (dbUrl != null) {
                m = getByDb(dbUrl);
            }
        }

        String text = doc.select("span.label-success").text();
        movie.setCompleted(text.contains("全剧完结") || text.contains("本季终"));
        movie.setName(doc.select("div.page-header h4").text().split("/")[0]);
        if (m != null) {
            Set<Resource> resources = m.getRes();
            int size = resources.size();
            m.addResources(findResource(doc, url));

            logger.info("[yy] get {}/{} resources for movie {}", (resources.size() - size), resources.size(),
                m.getName());
            service.save(m);
            m.setCompleted(movie.isCompleted());
            m.setSourceTime(movie.getSourceTime());
            return m;
        } else {
            findResource(doc, url);
        }

        logger.warn("Cannot find movie for {}: {}", movie.getName(), url);
        service.publishEvent(url, "Cannot find movie for " + movie.getName());
        return null;
    }

    private Set<Resource> findResource(Document doc, String pageUrl) {
        Set<Resource> resources = new HashSet<>();
        if (skipResource) {
            return resources;
        }

        Elements elements = doc.select("div.tab_set_info table.table tbody tr a img");
        if (elements.size() >= 50) {
            logger.info("Resources: {}", elements.size());
        }
        for (Element element : elements) {
            String uri = element.parent().attr("href");
            if (isResource(uri)) {
                String title = element.attr("title") + "-" + element.parent().parent().nextElementSibling().text();
                try {
                    resources.add(service.saveResource(uri, StringUtils.truncate(title, 120)));
                } catch (Exception e) {
                    service.publishEvent(pageUrl, e.getMessage());
                    logger.error("[yy] get resource failed", e);
                }
            } else if (!uri.trim().isEmpty()) {
                logger.warn("{} is not resource", uri);
            }
        }

//        logger.info("[yy] get {} resources", resources.size());
        elements = doc.select("div.tab_set_info ul li a");
        if (elements.size() >= 50) {
            logger.info("Resources: {}", elements.size());
        }
        for (Element element : elements) {
            String uri = element.attr("href");
            if (isResource(uri)) {
                String title = element.text();
                try {
                    resources.add(service.saveResource(uri, StringUtils.truncate(title, 120)));
                } catch (Exception e) {
                    service.publishEvent(pageUrl, e.getMessage());
                    logger.error("[yy] get resource failed", e);
                }
            } else if (!uri.trim().isEmpty()) {
                logger.warn("{} is not resource", uri);
            }
        }

        return resources;
    }

}
