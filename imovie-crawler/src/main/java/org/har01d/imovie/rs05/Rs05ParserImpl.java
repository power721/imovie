package org.har01d.imovie.rs05;

import java.io.IOException;
import java.util.Set;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.MovieRepository;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.domain.ResourceRepository;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.StringUtils;
import org.har01d.imovie.util.UrlUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

@Service
public class Rs05ParserImpl implements Rs05Parser {

    private static final Logger logger = LoggerFactory.getLogger(Rs05Parser.class);

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Override
    public Movie parse(String url, Movie movie) throws IOException {
        String html = HttpUtils.getHtml(url);
        Document doc = Jsoup.parse(html);
        Set<Resource> resources = movie.getResources();

        for (Element element : doc.select(".movie-txt a")) {
            Resource resource = findResource(element);
            if (resource != null) {
                resources.add(resource);
            }
        }

        for (Element element : doc.select(".resources a")) {
            Resource resource = findResource(element);
            if (resource != null) {
                resources.add(resource);
            }
        }

        logger.info("get {} resources for movie {}", resources.size(), movie.getName());

        movieRepository.save(movie);
        return movie;
    }

    private Resource findResource(Element element) {
        String uri = element.attr("href");
        if (!isResource(uri)) {
            return null;
        }

        String title = element.text();
        if (uri.contains("pan.baidu.com") && !title.contains("密码") && !title.contains("提取码")) {
            String temp = element.parent().text();
            if (temp.contains("密码") || title.contains("提取码")) {
                title = temp;
            }
        }
        title = StringUtils.truncate(title, 120);

        String newUri = UrlUtils.convertUrl(uri);
        Resource resource = resourceRepository.findFirstByUri(newUri);
        if (resource != null) {
            return resource;
        }

        Resource r = new Resource(newUri, title);
        if (!newUri.equals(uri)) {
            r.setOriginal(uri);
        }

        try {
            resourceRepository.save(r);
            logger.debug("find new resource {}", title);
            return r;
        } catch (JpaSystemException e) {
            logger.warn("save Resource failed!", e);
        }

        r = new Resource(uri, title);
        resourceRepository.save(r);
        logger.debug("find new resource {}", title);
        return r;
    }

    private boolean isResource(String uri) {
        return uri.startsWith("magnet") || uri.startsWith("ed2k://") || uri.startsWith("thunder://")
            || uri.startsWith("ftp://") || uri.contains("pan.baidu.com");
    }
}
