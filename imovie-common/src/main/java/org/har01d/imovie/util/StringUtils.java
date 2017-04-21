package org.har01d.imovie.util;

public final class StringUtils {

    public static String truncate(String text, int maxLen) {
        return text.substring(0, Math.min(text.length(), maxLen));
    }

}
