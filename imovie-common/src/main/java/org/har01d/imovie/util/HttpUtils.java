package org.har01d.imovie.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
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
    private static final Random RANDOM = new Random();
    private static final List<String> USER_AGENTS = new ArrayList<>();
    private static final List<Header> HEADERS = new ArrayList<>();

    static {
        HEADERS.add(new BasicHeader("Referer", "https://movie.douban.com/"));
        HEADERS.add(
            new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"));
        HEADERS.add(new BasicHeader("Accept-Language", "en-US,en;q=0.8,zh-CN;q=0.6,zh-TW;q=0.4"));
        HEADERS.add(new BasicHeader("Accept-Encoding", "gzip, deflate, sdch, br"));

        // https://udger.com/resources/ua-list
        USER_AGENTS.add(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36");
        USER_AGENTS.add("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:42.0) Gecko/20100101 Firefox/42.0");
        USER_AGENTS.add(
            "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.5; en-US; rv:1.9.1b3) Gecko/20090305 Firefox/3.1b3 GTB5");
        USER_AGENTS.add("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:33.0) Gecko/20100101 Firefox/33.0");
        USER_AGENTS.add("Mozilla/5.0 (Windows NT 10.0; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0");
        USER_AGENTS.add("Mozilla/5.0 (IE 11.0; Windows NT 6.3; WOW64; Trident/7.0; Touch; rv:11.0) like Gecko");
        USER_AGENTS.add("Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Win64; x64; Trident/6.0)");
        USER_AGENTS.add("Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; WOW64; Trident/6.0)");
        USER_AGENTS.add(
            "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.57 Safari/537.17 QIHU 360EE");
        USER_AGENTS.add(
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36 QIHU 360SE");
        USER_AGENTS.add(
            "Mozilla/5.0 (X11; Linux i686) AppleWebKit/535.7 (KHTML, like Gecko) Ubuntu/11.10 Chromium/16.0.912.21 Chrome/16.0.912.21 Safari/535.7");
        USER_AGENTS.add(
            "Mozilla/5.0 (Windows; U; Windows NT 6.1; zh_CN) AppleWebKit/534.7 (KHTML, like Gecko) Chrome/7.0 baidubrowser/1.x Safari/534.7");
        USER_AGENTS.add(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/34.0.1847.116 Chrome/34.0.1847.116 Safari/537.36");
        USER_AGENTS.add(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.38 Safari/537.36");
        USER_AGENTS.add(
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.71 Safari/537.36");
        USER_AGENTS.add(
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36");
        USER_AGENTS.add(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; Xbox; Xbox One) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586");
        USER_AGENTS.add(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.82 Safari/537.36 Edge/14.14359");
    }

    private static BasicCookieStore cookieStore = new BasicCookieStore();

    private static String getAgent() {
        int index = RANDOM.nextInt(USER_AGENTS.size());
        return USER_AGENTS.get(index);
    }

    public static String getHtml(String url) throws IOException {
        return getHtml(url, "UTF-8", cookieStore, null);
    }

    public static String getHtml(String url, HttpHost httpHost) throws IOException {
        return getHtml(url, "UTF-8", cookieStore, httpHost);
    }

    public static String getHtml(String url, String encoding, BasicCookieStore cookieStore) throws IOException {
        return getHtml(url, "UTF-8", cookieStore, null);
    }

    public static String getHtml(String url, String encoding, BasicCookieStore cookieStore, HttpHost httpHost)
        throws IOException {
        RequestConfig.Builder builder = RequestConfig.custom()
            .setConnectTimeout(CONNECTION_TIMEOUT_MS)
            .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
            .setSocketTimeout(SOCKET_TIMEOUT_MS);
        if (httpHost != null) {
            builder.setProxy(httpHost);
        }
        final RequestConfig requestConfig = builder.build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setDefaultCookieStore(cookieStore)
            .setDefaultHeaders(HEADERS)
            .setUserAgent(getAgent())
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
                HttpEntity entity = response.getEntity();
                LOGGER.debug(EntityUtils.toString(entity, encoding));
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
            .setDefaultHeaders(HEADERS)
            .setUserAgent(getAgent())
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

    public static String downloadFile(String url, List<NameValuePair> params, List<Header> headers, File file)
        throws IOException {
        HttpClientBuilder builder = HttpClients.custom().setUserAgent(getAgent());

        try (CloseableHttpClient httpClient = builder.build()) {
            RequestBuilder requestBuilder = RequestBuilder.post().setUri(url);
            if (params != null) {
                requestBuilder.addParameters(params.toArray(new NameValuePair[params.size()]));
            }
            if (headers != null) {
                for (Header header : headers) {
                    requestBuilder.addHeader(header);
                }
            }

            HttpUriRequest request = requestBuilder.build();
            LOGGER.info("Executing request {}", request.getRequestLine());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();
                Header header = response.getFirstHeader("Location");
                if (header != null) {
                    return header.getValue();
                }

                FileUtils.copyInputStreamToFile(entity.getContent(), file);
                LOGGER.info("download {} completed. {}", url, file);
            }
        }
        return null;
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

    public static String get(String url, String referer) throws IOException {
        String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36";
        final RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(CONNECTION_TIMEOUT_MS)
            .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
            .setSocketTimeout(SOCKET_TIMEOUT_MS)
            .build();
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Referer", referer));
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

    public static String get(String url, HttpHost proxy) throws IOException {
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(5000)
            .setConnectionRequestTimeout(5000)
            .setSocketTimeout(5000).build();
        HttpClientBuilder builder = HttpClients.custom()
            .setDefaultRequestConfig(config)
            .setUserAgent(getAgent())
            .setDefaultHeaders(HEADERS)
            .setProxy(proxy);

        try (CloseableHttpClient httpClient = builder.build()) {
            RequestBuilder requestBuilder = RequestBuilder.get().setUri(url);

            HttpUriRequest request = requestBuilder.build();
            LOGGER.info("[Proxy]Executing request {}", request.getRequestLine());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();

                return entity != null ? EntityUtils.toString(entity) : null;
            }
        }
    }

    public static String get(String url, Map<String, String> params) throws IOException {
        return get(url, params, null);
    }

    public static String get(String url, Map<String, String> params, BasicCookieStore cookieStore) throws IOException {
        HttpClientBuilder builder = HttpClients.custom().setUserAgent(getAgent());
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
            LOGGER.info("Executing request {}", request.getRequestLine());
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
        HttpClientBuilder builder = HttpClients.custom().setUserAgent(getAgent());
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
            LOGGER.info("Executing request {}", request.getRequestLine());
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
            .setUserAgent(getAgent())
            .setDefaultHeaders(HEADERS)
            .build()) {
            RequestBuilder requestBuilder = RequestBuilder.post().setUri(url);
            for (Entry<String, String> entry : params.entrySet()) {
                requestBuilder.addParameter(entry.getKey(), entry.getValue());
            }

            HttpUriRequest request = requestBuilder.build();
            LOGGER.info("Executing request {}", request.getRequestLine());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();
                String html = entity != null ? EntityUtils.toString(entity) : null;
                LOGGER.debug(html);
            }
        }
        return cookieStore;
    }

}
