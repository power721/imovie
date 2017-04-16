package org.har01d.imovie.rs05;

import java.io.IOException;
import org.har01d.imovie.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class Rs05CrawlerImpl implements Rs05Crawler {

    private static final Logger logger = LoggerFactory.getLogger(Rs05CrawlerImpl.class);

    @Value("${url.rs05}")
    private String baseUrl;

    @Override
    public void crawler() throws InterruptedException {
        int page = 1;
        while (true) {
            String url = baseUrl + page;
            try {
                String html = HttpUtils.getHtml(url);

            } catch (IOException e) {
                logger.error("Get HTML failed!", e);
            }
        }
    }
}
