package org.har01d.imovie.service;

import org.apache.http.HttpHost;

public interface ProxyService {

    void initProxies();

    HttpHost getProxy();
}
