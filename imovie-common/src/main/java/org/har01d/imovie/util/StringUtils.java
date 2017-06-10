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

    public static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n';
    }

    public static String convertFileSize(long size) {
        if (size >= 1024 * 1024 * 1024L) {
            return String.format("%.2fGB", (size / (1024 * 1024 * 1024D)));
        } else if (size >= 1024 * 1024L) {
            return String.format("%.2fMB", (size / (1024 * 1024D)));
        } else if (size >= 1024) {
            return String.format("%.2fKB", (size / 1024D));
        } else {
            return String.valueOf(size);
        }
    }

}
