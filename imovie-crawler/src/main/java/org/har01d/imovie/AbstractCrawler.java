package org.har01d.imovie;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractCrawler {

    protected int error;
    protected long total;
    protected Random random = new Random();

    @Autowired
    protected MovieService service;

    protected void sleep() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(random.nextInt(500));
    }

    protected int getPage(int defaultValue) {
        return getConfig("page", defaultValue);
    }

    protected int getPage() {
        return getPage(1);
    }

    protected void savePage(int page) {
        saveConfig("page", page);
    }

    protected int getPage(String type, int defaultValue) {
        return getConfig("page_" + type, defaultValue);
    }

    protected int getPage(String type) {
        return getPage(type, 1);
    }

    protected void savePage(String type, int page) {
        saveConfig("page_" + type, page);
    }

    protected void saveConfig(String name, int value) {
        service.saveConfig(getPageKey() + "_" + name, String.valueOf(value));
    }

    protected int getConfig(String name, int defaultValue) {
        String key = getPageKey() + "_" + name;
        Config config = service.getConfig(key);
        if (config == null) {
            return defaultValue;
        }

        return Integer.valueOf(config.getValue());
    }

    protected void deleteConfig(String name) {
        service.deleteConfig(getPageKey() + "_" + name);
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
