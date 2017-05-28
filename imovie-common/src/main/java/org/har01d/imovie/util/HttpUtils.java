package org.har01d.imovie.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class HttpUtils {

    public static final int CONNECTION_TIMEOUT_MS = 30 * 1000;
    public static final int CONNECTION_REQUEST_TIMEOUT_MS = 30 * 1000;
    public static final int SOCKET_TIMEOUT_MS = 30 * 1000;
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);
    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36";

    private static BasicCookieStore cookieStore = new BasicCookieStore();

    public static String getHtml(String url) throws IOException {
        return getHtml(url, "UTF-8", cookieStore);
    }

    public static String getHtml(String url, String encoding, BasicCookieStore cookieStore) throws IOException {
        RequestConfig.Builder builder = RequestConfig.custom()
            .setConnectTimeout(CONNECTION_TIMEOUT_MS)
            .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
            .setSocketTimeout(SOCKET_TIMEOUT_MS);
        final RequestConfig requestConfig = builder.build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setDefaultCookieStore(cookieStore)
            .setUserAgent(USER_AGENT)
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
                throw new HttpResponseException(status, "Unexpected response status: " + status);
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
        RequestConfig.Builder builder = RequestConfig.custom()
            .setConnectTimeout(CONNECTION_TIMEOUT_MS)
            .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
            .setSocketTimeout(SOCKET_TIMEOUT_MS);
        final RequestConfig requestConfig = builder.build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setDefaultCookieStore(cookieStore)
            .setUserAgent(USER_AGENT)
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
                throw new HttpResponseException(status, "Unexpected response status: " + status);
            }
        };

        try {
            httpClient.execute(httpget, responseHandler);
        } catch (IOException e) {
            LOGGER.warn("Download {} failed: {}, retrying...", uri, e.getMessage());
            httpClient.execute(httpget, responseHandler);
        } finally {
            IOUtils.closeQuietly(httpClient);
        }
    }

    public static String getJson(String url) throws IOException {
        return getJson(url, cookieStore);
    }

    public static String getJson(String url, BasicCookieStore cookieStore) throws IOException {
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
                throw new HttpResponseException(status, "Unexpected response status: " + status);
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

    public static String get(String url) throws IOException {
        return get(url, null, null);
    }

    public static String get(String url, Map<String, String> params) throws IOException {
        return get(url, params, null);
    }

    public static String get(String url, Map<String, String> params, BasicCookieStore cookieStore) throws IOException {
        LOGGER.debug("Get from {}", url);
        HttpClientBuilder builder = HttpClients.custom().setUserAgent(USER_AGENT);
        if (cookieStore != null) {
            builder.setDefaultCookieStore(cookieStore);
        }

        try (CloseableHttpClient httpClient = builder.build()) {
            RequestBuilder requestBuilder = RequestBuilder.get().setUri(url);
            if (params != null) {
                for (Entry<String, String> entry : params.entrySet()) {
                    requestBuilder.addParameter(entry.getKey(), entry.getValue());
                }
            }

            HttpUriRequest request = requestBuilder.build();
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();

                return entity != null ? EntityUtils.toString(entity) : null;
            }
        }
    }

    public static String post(String url) throws IOException {
        return post(url, null, null);
    }

    public static String post(String url, Map<String, String> params) throws IOException {
        return post(url, params, null);
    }

    public static String post(String url, Map<String, String> params, BasicCookieStore cookieStore) throws IOException {
        LOGGER.debug("Post to {}", url);
        HttpClientBuilder builder = HttpClients.custom().setUserAgent(USER_AGENT);
        if (cookieStore != null) {
            builder.setDefaultCookieStore(cookieStore);
        }

        try (CloseableHttpClient httpClient = builder.build()) {
            RequestBuilder requestBuilder = RequestBuilder.post().setUri(url);
            if (params != null) {
                for (Entry<String, String> entry : params.entrySet()) {
                    requestBuilder.addParameter(entry.getKey(), entry.getValue());
                }
            }

            HttpUriRequest request = requestBuilder.build();
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();

                return entity != null ? EntityUtils.toString(entity) : null;
            }
        }
    }

    public static BasicCookieStore post4Cookie(String url, Map<String, String> params) throws IOException {
        BasicCookieStore cookieStore = new BasicCookieStore();

        try (CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultCookieStore(cookieStore)
            .build()) {
            RequestBuilder requestBuilder = RequestBuilder.post().setUri(url);
            for (Entry<String, String> entry : params.entrySet()) {
                requestBuilder.addParameter(entry.getKey(), entry.getValue());
            }

            HttpUriRequest request = requestBuilder.build();
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();
                EntityUtils.consume(entity);
            }
        }
        return cookieStore;
    }

}
