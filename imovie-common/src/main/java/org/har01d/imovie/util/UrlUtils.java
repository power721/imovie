package org.har01d.imovie.util;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UrlUtils {

    public static final Pattern DB_PATTERN = Pattern.compile("(https?://movie\\.douban\\.com/subject/\\d+)");
    public static final Pattern IMDB_PATTERN = Pattern.compile("(https?://www\\.imdb\\.com/title/tt\\d+)");
    public static final Pattern IMDB = Pattern.compile("(tt\\d+)");

    private static final Logger logger = LoggerFactory.getLogger(UrlUtils.class);
    private static final Pattern MAGNET_PATTERN = Pattern
        .compile(
            "(magnet:\\?xt=urn:btih:[0-9a-zA-Z]+(&xt=[^ &]*)?(&dn=[^ &]*)?(&xl=[^ &]*)?(&tr=[^ &]*)*(&ws=[^ &]*)?)");
    private static final Pattern ED2K_PATTERN = Pattern
        .compile("(ed2k://\\|file\\|.+?\\|\\d+\\|[0-9a-zA-Z]+\\|(h=[0-9a-zA-Z]+\\|)?/)");

    public static List<String> findMagnet(String text) {
        List<String> urls = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return urls;
        }

        Matcher matcher = MAGNET_PATTERN.matcher(text);
        while (matcher.find()) {
            urls.add(matcher.group(1));
        }
        return urls;
    }

    public static List<String> findED2K(String text) {
        List<String> urls = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return urls;
        }

        Matcher matcher = ED2K_PATTERN.matcher(text);
        while (matcher.find()) {
            urls.add(matcher.group(1));
        }
        return urls;
    }

    public static String getDbUrl(String html) {
        int index = html.indexOf("movie.douban.com/subject/");
        if (index < 0) {
            return null;
        }


        index = index - "https://".length();
        if (index < 0) {
            index = 0;
        }
        String text = html.substring(index, Math.min(index + 45, html.length()));
        Matcher matcher = UrlUtils.DB_PATTERN.matcher(text);
        if (matcher.find()) {
            String url = matcher.group(1).replace("http://", "https://");
            if (url.endsWith("/")) {
                return url;
            } else {
                return url + "/";
            }
        }
        return null;
    }

    public static String getImdbUrl(String imdb) {
        if (imdb.contains("http://www.imdb.com/title/")) {
            Matcher matcher = UrlUtils.IMDB_PATTERN.matcher(imdb);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        Matcher matcher = UrlUtils.IMDB.matcher(imdb);
        if (matcher.find()) {
            return "http://www.imdb.com/title/" + matcher.group(1);
        }
        return null;
    }

    public static String convertUrl(String encodedUrl) {
        try {
            if (encodedUrl.startsWith("thunder://")) {
                String base64 = encodedUrl.substring("thunder://".length(), encodedUrl.length());
                byte[] bytes = Base64.getDecoder().decode(fixBase64(base64));
                String url = new String(bytes);
                return url.substring(2, url.length() - 2);
            }

            if (encodedUrl.startsWith("qqdl://")) {
                String base64 = encodedUrl.substring("qqdl://".length(), encodedUrl.length());
                byte[] bytes = Base64.getDecoder().decode(fixBase64(base64));
                return new String(bytes);
            }

            if (encodedUrl.startsWith("flashget://")) {
                String base64 = encodedUrl.substring("flashget://".length(), encodedUrl.length());
                byte[] bytes = Base64.getDecoder().decode(fixBase64(base64));
                String url = new String(bytes);
                return url.substring(10, url.length() - 10);
            }
        } catch (Exception e) {
            logger.warn("convert {} failed!", encodedUrl, e);
        }

        return encodedUrl;
    }

    private static String fixBase64(String base64) {
        if (base64.endsWith("/")) {
            return base64.substring(0, base64.length() - 1);
        }

        int index;
        char[] chars = base64.toCharArray();
        for (index = 0; index < chars.length; index++) {
            char c = chars[index];
            if (!isAlphabetic(c) && c != '+' && c != '/' && c != '=') {
                return base64.substring(0, index);
            }
        }

        return base64;
    }

    private static boolean isAlphabetic(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }
}
