package org.har01d.imovie.domain;

import java.util.List;

public interface MovieRepository {

    Movie get(Integer id);

    Movie save(Movie movie);

    List<Movie> findAll();
}
