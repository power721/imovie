package org.har01d.bittorrent;

import java.io.File;
import java.io.IOException;

public class TorrentFileTest {

    public static void main(String[] args) throws IOException {
        File file = new File("/tmp/bt/1.torrent");
        TorrentFile torrentFile = new TorrentFile(file);
        System.out.println(torrentFile.getName());
        System.out.println(torrentFile.getHexHash());
        System.out.println(torrentFile.getMagnet());
        System.out.println(torrentFile.getTracker());
        System.out.println(torrentFile.getTotalLength());
    }
}
