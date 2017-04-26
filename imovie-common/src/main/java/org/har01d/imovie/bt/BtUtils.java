package org.har01d.imovie.bt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;

public final class BtUtils {

    private static List<String> keyList;

    static {
        String[] keys = {"announce", "announce-list", "creation date", "comment", "created by",
            "info", "length", "md5sum", "name", "piece length", "pieces", "files", "path"};
        keyList = Arrays.asList(keys);
    }

    public static String torrent2Magnet(File file) throws IOException {
        int start = 0;
        int end;
        String name = "";

        try (InputStream is = new FileInputStream(file)) {
            int total = is.available();
            end = total - 1;
            String key = null;
            StringBuilder strLengthBuilder = new StringBuilder();
            int tempByte;

            while ((tempByte = is.read()) != -1) {
                char temp = (char) tempByte;
                switch (temp) {
                    case 'i':
                        while ((char) is.read() != 'e') {
                            // do nothing
                        }
                        break;
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        strLengthBuilder.append(temp);
                        break;
                    case ':':
                        if (strLengthBuilder.length() == 0) {
                            System.err.println("skip " + is.available());
//                            end = total - is.available();  // TODO: check this
//                            is.skip(total);
//                            break;
                            return null;
                        }

                        int strLen = Integer.parseInt(strLengthBuilder.toString());
                        strLengthBuilder = new StringBuilder();
                        byte[] tempBytes = new byte[strLen];
                        is.read(tempBytes);
                        String tempStr = new String(tempBytes);
                        if ("nodes".equals(tempStr)) {
                            end = total - is.available() - 7;
                            break;
                        }

                        if (key == null || !key.equals("pieces")) {
                            if (keyList.contains(tempStr)) {  // key
                                key = tempStr;
                                if (tempStr.equals("info")) {
                                    start = total - is.available();
                                }
                            } else {  // value
                                if ("name".equals(key)) {
                                    name = tempStr;
                                }
                            }
                        }
                        break;
                }
            }
        }

        try (InputStream is2 = new FileInputStream(file)) {
            byte[] bytes = new byte[end - start];
            is2.skip(start);
            is2.read(bytes);
            String sha1 = DigestUtils.sha1Hex(bytes).toUpperCase();
            return "magnet:?xt=urn:btih:" + sha1 + "&dn=" + name;
        }
    }

}
