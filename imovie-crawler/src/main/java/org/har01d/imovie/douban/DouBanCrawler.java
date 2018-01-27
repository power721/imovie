package org.har01d.imovie.douban;

import org.har01d.imovie.Crawler;
import org.json.simple.JSONObject;

public interface DouBanCrawler extends Crawler {
    void updateDbScore(JSONObject item);
    void updateDbRate(JSONObject item);
}
