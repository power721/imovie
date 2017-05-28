package org.har01d.imovie.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpHost;
import org.har01d.imovie.domain.Proxy;
import org.har01d.imovie.domain.ProxyRepository;
import org.har01d.imovie.util.HttpUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProxyServiceImpl implements ProxyService {

    private static final Logger logger = LoggerFactory.getLogger(ProxyService.class);
    private static final Pattern PATTERN1 = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+) (\\d+) .* (HTTPS?) .*");
    private static final Pattern PATTERN2 = Pattern
        .compile("(\\d+\\.\\d+\\.\\d+\\.\\d+) (\\d+) .* .* \\d+-\\d+-\\d+ .*");
    private List<Proxy> proxies = new ArrayList<>();
    private volatile int index;

    @Autowired
    private ProxyRepository repository;

    @Override
    public void initProxies() {
        proxies = repository.findAll();
        if (proxies.size() < 3) {
//            getProxy1();
//            getProxy2();
//            getProxy3();
            getProxy4();
        }
        logger.info("get {} proxies", proxies.size());
    }

    @Override
    public synchronized HttpHost getProxy() {
        if (proxies.isEmpty()) {
            return null;
        }

        if (index >= proxies.size()) {
            index = 0;
        }

        Proxy proxy = proxies.get(index++);
        if (index >= proxies.size()) {
            index = 0;
        }
        return proxy.toHttpHost();
    }

    private void getProxy1() {
        try {
            String html = HttpUtils.getHtml("http://www.xicidaili.com/");
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("table#ip_list tr");
            getProxy(elements);
        } catch (IOException e) {
            logger.warn("get proxy list failed.", e);
        }
    }

    private void getProxy2() {
        try {
            String html = HttpUtils.getHtml("http://www.mimiip.com/");
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("table.list tr");
            getProxy(elements);
        } catch (IOException e) {
            logger.warn("get proxy list failed.", e);
        }
    }

    private void getProxy3() {
        try {
            String html = HttpUtils.getHtml("http://cn-proxy.com/");
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("table#tablekit-table-1 tr");
            for (Element element : elements) {
                String text = element.text().trim();
                Matcher m = PATTERN2.matcher(text);
                if (m.find()) {
                    String hostname = m.group(1);
                    int port = Integer.valueOf(m.group(2));
                    HttpHost httpHost = new HttpHost(hostname, port);
                    try {
                        html = HttpUtils.get("https://movie.douban.com/", httpHost);
                        logger.debug(html);
                        if (html != null && html.contains("m.douban.com")) {
                            Proxy proxy = new Proxy(hostname, port);
                            proxies.add(repository.save(proxy));
                            logger.info("add proxy " + httpHost);
                        }
                    } catch (IOException e) {
                        logger.warn("proxy " + hostname + ":" + port + " is not available: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("get proxy list failed.", e);
        }
    }

    private void getProxy4() {
        try {
            String html = HttpUtils.getHtml("http://www.kuaidaili.com/free/");
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("div#list table tr");
            getProxy(elements);
        } catch (IOException e) {
            logger.warn("get proxy list failed.", e);
        }
    }

    private void getProxy(Elements elements) {
        for (Element element : elements) {
            String text = element.text().trim();
            Matcher m = PATTERN1.matcher(text);
            if (m.find()) {
                String hostname = m.group(1);
                int port = Integer.valueOf(m.group(2));
                String scheme = m.group(3);
                HttpHost httpHost = new HttpHost(hostname, port, scheme);
                try {
                    String html = HttpUtils.get("https://movie.douban.com/", httpHost);
                    logger.debug(html);
                    if (html != null && html.contains("m.douban.com")) {
                        Proxy proxy = new Proxy(hostname, port, scheme);
                        proxies.add(repository.save(proxy));
                        logger.info("add proxy " + httpHost);
                    }
                } catch (IOException e) {
                    logger.warn("proxy " + hostname + ":" + port + " is not available: " + e.getMessage());
                }
            }
        }
    }

}
