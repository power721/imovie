package org.har01d.bittorrent;

import java.io.File;
import java.io.IOException;

public class TorrentFileTest {

    public static void main(String[] args) throws IOException {
        printInfo("/tmp/bt/0.torrent");
        printInfo("/tmp/bt/1.torrent");
        printInfo("/tmp/bt/55.torrent");
    }

    private static void printInfo(String fileName) throws IOException {
        File file = new File(fileName);
        TorrentFile torrentFile = new TorrentFile(file);
        System.out.println("Name: " + torrentFile.getName());
        System.out.println("Hash: " + torrentFile.getHexHash());
        System.out.println("Magnet: " + torrentFile.getMagnet());
        System.out.println("Tracker: " + torrentFile.getTracker());
        System.out.println("File Size: " + torrentFile.getTotalLength() + " bytes");
        System.out.println("Piece Length: " + torrentFile.getPieceLength() + " bytes");
        System.out.println("Number of Pieces: " + torrentFile.getNumPieces());
        System.out.println("Info Hash: " + torrentFile.getInfoHash());
        String[] fileNames = torrentFile.getFilenames();
        System.out.println(fileNames.length + " files:");
        for (int i = 0; i < fileNames.length; ++i) {
            System.out
                .println("  file " + (i + 1) + ": " + fileNames[i] + "  " + torrentFile.getLengths()[i] + " bytes");
        }
        System.out.println("=======================================================\n");
    }

}
