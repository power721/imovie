package org.har01d.imovie;

import java.util.Date;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractCrawler {

    @Autowired
    protected MovieService service;

    protected int getPage(int defaultValue) {
        Config config = service.getConfig(getPageKey() + "_page");
        if (config == null) {
            return defaultValue;
        }

        return Integer.valueOf(config.getValue());
    }

    protected int getPage() {
        return getPage(1);
    }

    protected void savePage(int page) {
        service.saveConfig(getPageKey() + "_page", String.valueOf(page));
    }

    protected int getPage(String type, int defaultValue) {
        String key = getPageKey() + "_page_" + type;
        Config config = service.getConfig(key);
        if (config == null) {
            return defaultValue;
        }

        return Integer.valueOf(config.getValue());
    }

    protected int getPage(String type) {
        return getPage(type, 1);
    }

    protected void savePage(String type, int page) {
        service.saveConfig(getPageKey() + "_page_" + type, String.valueOf(page));
    }

    protected Config getCrawlerConfig() {
        return service.getConfig(getPageKey() + "_crawler");
    }

    protected Config getCrawlerConfig(String type) {
        return service.getConfig(getPageKey() + "_crawler_" + type);
    }

    protected Config saveCrawlerConfig() {
        return service.saveConfig(getPageKey() + "_crawler", new Date().toString());
    }

    protected Config saveCrawlerConfig(String type) {
        return service.saveConfig(getPageKey() + "_crawler_" + type, new Date().toString());
    }

    private String getPageKey() {
        return getClass().getSimpleName().replace("CrawlerImpl", "").toLowerCase();
    }

}
