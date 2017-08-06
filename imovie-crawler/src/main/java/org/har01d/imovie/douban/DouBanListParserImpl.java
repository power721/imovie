package org.har01d.imovie.douban;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.MovieCollection;
import org.har01d.imovie.domain.MovieCollectionRepository;
import org.har01d.imovie.domain.MovieComment;
import org.har01d.imovie.domain.MovieCommentRepository;
import org.har01d.imovie.domain.Source;
import org.har01d.imovie.service.MovieService;
import org.har01d.imovie.util.HttpUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DouBanListParserImpl implements DouBanListParser {

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");

    @Autowired
    private DouBanParser douBanParser;

    @Autowired
    private MovieService service;

    @Autowired
    private MovieCollectionRepository movieCollectionRepository;

    @Autowired
    private MovieCommentRepository movieCommentRepository;

    @Override
    public boolean parse(String url) throws IOException, ParseException {
        int start = 0;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        MovieCollection movieCollection = new MovieCollection();
        movieCollection.setUrl(url);
        List<MovieComment> movieComments = movieCollection.getMovieComments();

        int count = 0;
        while (true) {
            String uri = url + "?start=" + start + "&sort=time";
            String html = HttpUtils.getHtml(uri);
            Document doc = Jsoup.parse(html);
            if (start == 0) {
                String followers = doc.select("#content div.article .doulist-collect a.doulist-followers-link").text();
                try {
                    movieCollection.setFollowers(Integer.parseInt(followers));
                } catch (NumberFormatException e) {
                    return false;
                }
                if (movieCollection.getFollowers() < 5000) {
                    return false;
                }

                String rec = doc.select("#content div.article .doulist-panel .rec-sec span.rec-num").text();
                try {
                    movieCollection.setRecommend(Integer.parseInt(rec.replace("人", "")));
                } catch (NumberFormatException e) {
                    return false;
                }
                if (movieCollection.getRecommend() < 100) {
                    return false;
                }

                movieCollection.setFollowers(Integer.parseInt(followers));

                String title = doc.select("#content h1").text();
                movieCollection.setTitle(title);
                String author = doc.select("#doulist-info div.meta a").text();
                movieCollection.setAuthor(author);
                String time = doc.select("#doulist-info div.meta span.time").text();
                Matcher matcher = DATE_PATTERN.matcher(time);
                if (matcher.find()) {
                    Date date = dateFormat.parse(matcher.group(1));
                    movieCollection.setCreatedTime(date);
                }
                if (matcher.find()) {
                    Date date = dateFormat.parse(matcher.group(1));
                    movieCollection.setUpdatedTime(date);
                }
                String synopsis = doc.select("#link-report div.doulist-about").text();
                movieCollection.setSynopsis(synopsis);
                log.info("{}: {} {}", title, followers, rec);
            }

            Elements elements = doc.select("#content div.article .doulist-item");
            if (elements.size() == 0) {
                break;
            }

            for (Element element : elements) {
                String dbUrl = element.select("div.title a").attr("href");
                if (!dbUrl.contains("//movie.douban.com/subject/")) {
                    continue;
                }
                log.info("{}:{} {}", movieCollection.getTitle(), count, dbUrl);
                Movie movie = getMovieByDb(dbUrl);
                if (movie != null) {
                    String comment = element.select("div.comment-item .comment").text();
                    MovieComment movieComment = new MovieComment();
                    movieComment.setMovie(movie);
                    movieComment.setComment(comment);
                    movieComments.add(movieCommentRepository.save(movieComment));
                    count++;
                }
            }

            start += 25;
        }

        movieCollectionRepository.save(movieCollection);
        service.save(new Source(url));
        log.info("{}-{}: {}", url, movieCollection.getTitle(), count);
        return true;
    }

    private Movie getMovieByDb(String dbUrl) {
        Movie movie = null;
        if (dbUrl != null) {
            movie = service.findByDbUrl(dbUrl);
            if (movie == null) {
                try {
                    movie = douBanParser.parse(dbUrl);
                    if (movie != null) {
                        return service.save(movie);
                    }
                } catch (IOException e) {
                    service.publishEvent(dbUrl, e.getMessage());
                    log.warn("parse DouBan failed.", e);
                }
            }
        }
        return movie;
    }

}
