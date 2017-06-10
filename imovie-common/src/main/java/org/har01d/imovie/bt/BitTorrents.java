package org.har01d.imovie.bt;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;

public class BitTorrents {

    public static BitTorrentInfo parse(File btFile) throws Exception {
        return new BitTorrents().analyze(btFile);
    }

    public static BitTorrentInfo parse(String btFilePath) throws Exception {
        return new BitTorrents().analyze(new File(btFilePath));
    }

    private BitTorrentInfo analyze(File file) throws Exception {
        BitTorrentInfo btInfo = new BitTorrentInfo();
        String key = null;
        StringBuilder strLengthBuilder = new StringBuilder();
        int start = 0;
        int end;
        int tempByte;

        try (InputStream is = new FileInputStream(file)) {
            int total = is.available();
            end = total - 1;
            while ((tempByte = is.read()) != -1) {
                char temp = (char) tempByte;
                switch (temp) {
                    case 'i':
                        StringBuilder itempBuilder = new StringBuilder();
                        char iTemp;
                        while ((iTemp = (char) is.read()) != 'e') {
                            itempBuilder.append(iTemp);
                        }
                        btInfo.setValue(key, itempBuilder.toString());
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
                            System.out.println("skip " + is.available());
                            end = total - is.available();
                            is.skip(total);
                            break;
                        }
                        int strLen = Integer.parseInt(strLengthBuilder.toString());
                        strLengthBuilder = new StringBuilder();
                        byte[] tempBytes = new byte[strLen];
                        is.read(tempBytes);
                        String tempStr = new String(tempBytes);
                        if ("nodes".equals(tempStr)) {
                            end = total - is.available() - 7;
                        }
                        if (key != null && key.equals("pieces")) {
                            btInfo.setValue(key, tempBytes);
                        } else {
                            if (BitTorrentInfo.keyList.contains(tempStr)) {
                                key = tempStr;
                                if (tempStr.equals("announce-list")) {
                                    btInfo.setAnnounceList(new LinkedList<String>());
                                } else if (tempStr.equals("info")) {
                                    btInfo.setInfo(new Info());
                                    start = total - is.available();
                                } else if (tempStr.equals("files")) {
                                    btInfo.getInfo().setFiles(new LinkedList<Files>());
                                    btInfo.getInfo().getFiles().add(new Files());
                                } else if (tempStr.equals("length")) {
                                    List<Files> tempFiles = btInfo.getInfo().getFiles();
                                    if (tempFiles != null) {
                                        if (tempFiles.isEmpty()
                                            || tempFiles.get(tempFiles.size() - 1).getLength() != 0) {
                                            tempFiles.add(new Files());
                                        }
                                    }
                                } else if (tempStr.equals("md5sum")) {
                                    List<Files> tempFiles = btInfo.getInfo().getFiles();
                                    if (tempFiles != null) {
                                        if (tempFiles.isEmpty()
                                            || tempFiles.get(tempFiles.size() - 1).getMd5sum() != null) {
                                            tempFiles.add(new Files());
                                        }
                                    }
                                } else if (tempStr.equals("path")) {
                                    List<Files> tempFilesList = btInfo.getInfo().getFiles();
                                    if (tempFilesList.isEmpty()) {
                                        Files files = new Files();
                                        files.setPath(new LinkedList<String>());
                                        tempFilesList.add(files);
                                    } else {
                                        Files files = tempFilesList.get(tempFilesList.size() - 1);
                                        if (files.getPath() == null) {
                                            files.setPath(new LinkedList<String>());
                                        }
                                    }
                                }
                            } else {
                                btInfo.setValue(key, tempStr);
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
            btInfo.setSha1(DigestUtils.sha1Hex(bytes).toUpperCase());
            btInfo.setMagnet("magnet:?xt=urn:btih:" + btInfo.getSha1() + "&dn=" + btInfo.getInfo().getName());
        }

        if (btInfo.getInfo().getFiles() != null) {
            long size = 0;
            for (Files fileInfo : btInfo.getInfo().getFiles()) {
                size += fileInfo.getLength();
            }
            btInfo.setFileSize(size);
        }

        return btInfo;
    }

    public static void main(String[] args) throws Exception {
//        FilenameFilter filter = (dir, name) -> name.endsWith(".torrent");
//        File dir = new File("/media/harold/Download/TDDOWNLOAD/bt/");
//        File output = new File("/media/harold/Download/TDDOWNLOAD/bt/magnet1.txt");
//        output.createNewFile();
//        try (FileWriter fileWriter = new FileWriter(output)) {
//            for (File file : dir.listFiles(filter)) {
//                System.out.println(file);
//                fileWriter.write(file.getName());
//                fileWriter.write(": ");
//                fileWriter.write(String.valueOf(BtUtils.torrent2Magnet(file)));
//                fileWriter.write("\n");
//            }
//        }

        BitTorrentInfo info = parse(
            "/tmp/bt/1-1.torrent");
        System.out.println("SHA1: " + info.getSha1());
        System.out.println("Magnet: " + info.getMagnet());
        System.out.println(
            "信息:" + info.getAnnounce() + "\t" + info.getComment() + "\t" + info.getCreateBy() + "\t" + timestamp2Date(
                info.getCreationDate()));
        Info it = info.getInfo();
        System.out.println(
            "信息1:" + it.getName() + "\t" + it.getPiecesLength() + "\t" + it.getLength() + "\t" + it.getMd5sum() + "\t"
                + it.getPieces());
        if (info.getAnnounceList() != null) {
            for (String str : info.getAnnounceList()) {
                System.out.println("信息2:" + str);
            }
        }
        if (it.getFiles() != null) {
            int i = 0;
            for (Files file : it.getFiles()) {
                i++;
                System.out.println("文件" + i + " 信息3:" + file.getLength() + "\t" + file.getMd5sum());
                if (file.getPath().size() > 0) {
                    for (String str : file.getPath()) {
                        System.out.println("文件" + i + " 信息4：" + str);
                    }
                }
            }
        }
    }

    private static Date timestamp2Date(long timestamp) {
        Date date = new Date(timestamp);
        return date;
    }
}
