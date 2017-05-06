package org.har01d.imovie.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class HttpUtils {

    public static final int CONNECTION_TIMEOUT_MS = 30 * 1000;
    public static final int CONNECTION_REQUEST_TIMEOUT_MS = 30 * 1000;
    public static final int SOCKET_TIMEOUT_MS = 30 * 1000;
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);

    private static BasicCookieStore cookieStore = new BasicCookieStore();

    static {
        BasicClientCookie cookie = new BasicClientCookie("dbcl2", "62974743:pc6WLag6Vww");
        cookie.setDomain(".movie.douban.com");
        cookie.setPath("/");
        cookieStore.addCookie(cookie);
    }

    public static String getHtml(String url) throws IOException {
        return getHtml(url, "UTF-8");
    }

    public static String getHtml(String url, String encoding) throws IOException {
        String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36";
        RequestConfig.Builder builder = RequestConfig.custom()
            .setConnectTimeout(CONNECTION_TIMEOUT_MS)
            .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
            .setSocketTimeout(SOCKET_TIMEOUT_MS);
        final RequestConfig requestConfig = builder.build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setDefaultCookieStore(cookieStore)
            .setUserAgent(userAgent)
            .build();
        HttpGet httpget = new HttpGet(url);

        LOGGER.info("Executing request {}", httpget.getRequestLine());

        // Create a custom response handler
        ResponseHandler<String> responseHandler = response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity, encoding) : null;
            } else if (status >= 500 && status <= 599) {
                throw new IOException("Unexpected response status: " + status);
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        };

        try {
            return httpClient.execute(httpget, responseHandler);
        } catch (IOException e) {
            LOGGER.warn("Parse {} failed: {}, retrying...", url, e.getMessage());
            return httpClient.execute(httpget, responseHandler);
        } finally {
            IOUtils.closeQuietly(httpClient);
        }
    }

    public static void downloadFile(String uri, File file) throws IOException {
        String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36";
        RequestConfig.Builder builder = RequestConfig.custom()
            .setConnectTimeout(CONNECTION_TIMEOUT_MS)
            .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
            .setSocketTimeout(SOCKET_TIMEOUT_MS);
        final RequestConfig requestConfig = builder.build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setDefaultCookieStore(cookieStore)
            .setUserAgent(userAgent)
            .build();
        HttpGet httpget = new HttpGet(uri);

        LOGGER.info("Executing request {}", httpget.getRequestLine());

        // Create a custom response handler
        ResponseHandler<Void> responseHandler = response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                FileUtils.copyInputStreamToFile(entity.getContent(), file);
                LOGGER.info("download {} completed. {}", uri, file);
                return null;
            } else if (status >= 500 && status <= 599) {
                throw new IOException("Unexpected response status: " + status);
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        };

        try {
            httpClient.execute(httpget, responseHandler);
        } catch (IOException e) {
            LOGGER.warn("Parse {} failed: {}, retrying...", uri, e.getMessage());
            httpClient.execute(httpget, responseHandler);
        } finally {
            IOUtils.closeQuietly(httpClient);
        }
    }

    public static String getJson(String url) throws IOException {
        String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36";
        final RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(CONNECTION_TIMEOUT_MS)
            .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
            .setSocketTimeout(SOCKET_TIMEOUT_MS)
            .build();
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Accept", "application/json"));
        headers.add(new BasicHeader("X-Request", "JSON"));
        headers.add(new BasicHeader("X-Requested-With", "XMLHttpRequest"));
        CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setDefaultCookieStore(cookieStore)
            .setDefaultHeaders(headers)
            .setUserAgent(userAgent)
            .build();
        HttpGet httpget = new HttpGet(url);

        LOGGER.info("Executing request {}", httpget.getRequestLine());

        // Create a custom response handler
        ResponseHandler<String> responseHandler = response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else if (status >= 500 && status <= 599) {
                throw new IOException("Unexpected response status: " + status);
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        };

        try {
            return httpClient.execute(httpget, responseHandler);
        } catch (IOException e) {
            LOGGER.warn("Parse {} failed: {}, retrying...", url, e.getMessage());
            return httpClient.execute(httpget, responseHandler);
        } finally {
            IOUtils.closeQuietly(httpClient);
        }
    }

}
