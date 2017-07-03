package org.har01d.imovie;

import org.har01d.imovie.domain.Config;
import org.har01d.imovie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractCrawler {

    @Autowired
    protected MovieService service;

    protected int getPage(int defaultValue) {
        Config config = service.getConfig(getPageKey());
        if (config == null) {
            return defaultValue;
        }

        return Integer.valueOf(config.getValue());
    }

    protected int getPage() {
        return getPage(1);
    }

    protected void savePage(int page) {
        service.saveConfig(getPageKey(), String.valueOf(page));
    }

    protected void updateCrawlerTime(Config crawler) {
        if (crawler != null) {
            crawler.setValue(String.valueOf(System.currentTimeMillis()));
            service.save(crawler);
        }
    }

    protected abstract String getPageKey();

}
