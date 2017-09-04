package org.har01d.imovie.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtils {

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})-\\d{2}-\\d{2}");
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\s*(\\d{4})\\D*");

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

    public static Integer getYear(String yearStr) {
        if (yearStr == null) {
            return null;
        }

        Matcher matcher = DATE_PATTERN.matcher(yearStr);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }

        matcher = YEAR_PATTERN.matcher(yearStr);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }
        return null;
    }

}
