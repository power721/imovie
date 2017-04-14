package org.har01d.imovie.domain;

public interface MovieRepository {

    Movie get(Integer id);

    Movie save(Movie movie);
}
