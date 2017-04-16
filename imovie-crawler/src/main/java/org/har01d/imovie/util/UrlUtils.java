package org.har01d.imovie.util;

import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UrlUtils {

    private static final Logger logger = LoggerFactory.getLogger(UrlUtils.class);

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
