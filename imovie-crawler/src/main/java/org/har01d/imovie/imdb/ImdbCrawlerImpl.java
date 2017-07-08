package org.har01d.imovie.imdb;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import org.har01d.imovie.AbstractCrawler;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.Imdb;
import org.har01d.imovie.domain.ImdbRepository;
import org.har01d.imovie.util.HttpUtils;
import org.har01d.imovie.util.UrlUtils;
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
public class ImdbCrawlerImpl extends AbstractCrawler implements ImdbCrawler {

    private static final Logger logger = LoggerFactory.getLogger(ImdbCrawlerImpl.class);

    @Value("${url.imdb}")
    private String baseUrl;

    @Autowired
    private ImdbRepository repository;

    private Map<String, String> sort = new HashMap<>();
    private String[] genres = new String[]{"", "Action", "Adventure", "Animation", "Biography", "Comedy", "Crime",
        "Documentary", "Drama", "Family", "Fantasy", "Film-Noir", "History", "Horror", "Music", "Musical", "Mystery",
        "Romance", "Sci-Fi", "Short", "Sport", "Thriller", "War", "Western"};

    public ImdbCrawlerImpl() {
        sort.put("pop", "");
        sort.put("rating", "user_rating,desc");
        sort.put("vote", "num_votes,desc");
//        sort.put("alpha1", "alpha,asc");
//        sort.put("alpha2", "alpha,desc");
        sort.put("gross", "boxoffice_gross_us,desc");
//        sort.put("runtime", "runtime,desc");
    }

    @Override
    public void crawler() throws InterruptedException {
        for (String type : sort.keySet()) {
            int i = getIndex(type);
            for (; i < genres.length; ++i) {
                work(type, genres[i]);
                saveIndex(type, i + 1);
            }
        }
    }

    private void work(String type, String genre) {
        int page = getPage(type);
        while (page <= 100) {
            String url = baseUrl + page + "&sort=" + sort.get(type);
            if (genre != null) {
                url = url + "&genres=" + genre;
            }
            logger.info("get imdb for {}-{}-{}", type, genre, page);

            try {
                String html = HttpUtils.getHtml(url);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.select(".lister-list .lister-item .lister-col-wrapper");
                if (elements.size() == 0) {
                    break;
                }

                for (Element element : elements) {
                    String href = element.select(".col-title .lister-item-header a").attr("href");
                    String imdb = getImdb(href);
                    String rating = element.select(".col-imdb-rating").text();
                    if ("-".equals(rating)) {
                        rating = null;
                    }
                    repository.save(new Imdb(imdb, rating));
                }
                page++;
                savePage(type, page);
            } catch (IOException e) {
                service.publishEvent(url, e.getMessage());
                logger.error("[imdb] Get HTML failed: " + url, e);
            }
        }
        savePage(type, 1);
    }

    private String getImdb(String imdb) {
        Matcher matcher = UrlUtils.IMDB.matcher(imdb);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private int getIndex(String type) {
        String key = "imdb_index_" + type;
        Config config = service.getConfig(key);
        if (config == null) {
            return 0;
        }

        return Integer.valueOf(config.getValue());
    }

    private void saveIndex(String type, int index) {
        service.saveConfig("imdb_index_" + type, String.valueOf(index));
    }

}
