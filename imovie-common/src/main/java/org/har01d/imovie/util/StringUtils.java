package org.har01d.imovie.util;

public final class StringUtils {

    public static String truncate(String text, int maxLen) {
        if (text == null) {
            return null;
        }

        if (text.length() > maxLen) {
            return text.substring(0, maxLen) + "...";
        }
        return text;
    }

}
