package org.har01d.imovie.bt;

public class StringUtil {

    public static boolean isNumeric(String s) {
        for (char c : s.toCharArray()) {
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
