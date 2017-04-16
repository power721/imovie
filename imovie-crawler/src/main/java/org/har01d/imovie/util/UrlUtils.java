package org.har01d.imovie.util;

import java.util.Base64;

public final class UrlUtils {

    public static String convertUrl(String encodedUrl) {
        if (encodedUrl.startsWith("thunder://")) {
            byte[] bytes = Base64.getDecoder().decode(encodedUrl.substring("thunder://".length(), encodedUrl.length()));
            String url = new String(bytes);
            return url.substring(2, url.length() - 2);
        }

        if (encodedUrl.startsWith("qqdl://")) {
            byte[] bytes = Base64.getDecoder().decode(encodedUrl.substring("qqdl://".length(), encodedUrl.length()));
            return new String(bytes);
        }

        if (encodedUrl.startsWith("flashget://")) {
            byte[] bytes = Base64.getDecoder()
                .decode(encodedUrl.substring("flashget://".length(), encodedUrl.length()));
            String url = new String(bytes);
            return url.substring(10, url.length() - 10);
        }

        return encodedUrl;
    }

}
