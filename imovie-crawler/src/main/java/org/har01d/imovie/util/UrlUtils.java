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
                byte[] bytes = Base64.getDecoder().decode(base64);
                String url = new String(bytes);
                return url.substring(2, url.length() - 2);
            }

            if (encodedUrl.startsWith("qqdl://")) {
                String base64 = encodedUrl.substring("qqdl://".length(), encodedUrl.length());
                byte[] bytes = Base64.getDecoder().decode(base64);
                return new String(bytes);
            }

            if (encodedUrl.startsWith("flashget://")) {
                String base64 = encodedUrl.substring("flashget://".length(), encodedUrl.length());
                byte[] bytes = Base64.getDecoder().decode(base64);
                String url = new String(bytes);
                return url.substring(10, url.length() - 10);
            }
        } catch (Exception e) {
            logger.warn("convert {} failed!", encodedUrl, e);
        }

        return encodedUrl;
    }

}
