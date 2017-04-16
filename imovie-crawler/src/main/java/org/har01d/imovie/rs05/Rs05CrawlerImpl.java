package org.har01d.imovie.rs05;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.MovieRepository;
import org.har01d.imovie.domain.Tag;
import org.har01d.imovie.domain.TagRepository;
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
public class Rs05CrawlerImpl implements Rs05Crawler {

    private static final Logger logger = LoggerFactory.getLogger(Rs05Crawler.class);
    private static final Pattern TITLE_PATTERN = Pattern.compile("《(.*)》.*");

    @Value("${url.rs05}")
    private String baseUrl;

    @Autowired
    private Rs05Parser parser;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TagRepository tagRepository;

    @Override
    public void crawler() throws InterruptedException {
        int page = 1;
        while (true) {
            String url = baseUrl + page;
            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select("#movielist li");
                logger.info("get {} movies", elements.size());
                for (Element element : elements) {
                    Element header = element.select(".intro h2 a").first();
                    String title = header.attr("title");
                    String name = getName(title);

                    String pageUrl = header.attr("href");
                    String cover = element.select(".movie-thumbnails img").attr("data-original");
                    Element dou = element.select(".intro .dou a").first();
                    String dbUrl = dou.attr("href");
                    String dbScore = dou.text();
                    Elements tagElements = element.select(".tags a");

                    if (movieRepository.findFirstBySource(pageUrl).isPresent()) {
                        continue;
                    }

                    Movie movie = new Movie();
                    movie.setSource(pageUrl);
                    movie.setCover(cover);
                    movie.setTitle(title);
                    movie.setName(name);
                    movie.setDbUrl(dbUrl);
                    movie.setDbScore(dbScore);
                    movie.setTags(getTags(tagElements));
                    parser.parse(pageUrl, movie);
                }
                page++;
            } catch (IOException e) {
                logger.error("Get HTML failed!", e);
            }
        }
    }

    private String getName(String title) {
        Matcher matcher = TITLE_PATTERN.matcher(title);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return title;
    }

    private Set<Tag> getTags(Elements tagElements) {
        Set<Tag> tags = new HashSet<>();
        for (Element element : tagElements) {
            String name = element.text().replace("#", "");
            Optional<Tag> tag = tagRepository.findFirstByName(name);
            if (tag.isPresent()) {
                tags.add(tag.get());
            } else {
                Tag t = new Tag(name);
                tagRepository.save(t);
                tags.add(t);
            }
        }
        return tags;
    }

}
