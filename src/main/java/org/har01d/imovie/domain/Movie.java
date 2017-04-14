package org.har01d.imovie.domain;

import java.util.concurrent.atomic.AtomicInteger;

public class Movie {
    private static AtomicInteger idGenerator = new AtomicInteger();

    private Integer id;
    private String name;

    public Movie(String name) {
        this.id = idGenerator.incrementAndGet();
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Movie{" +
            "id=" + id +
            ", name='" + name + '\'' +
            '}';
    }
}
