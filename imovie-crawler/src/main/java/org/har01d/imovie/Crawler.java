package org.har01d.imovie;

public interface Crawler {
    void crawler() throws InterruptedException;

    boolean isNew();
}
