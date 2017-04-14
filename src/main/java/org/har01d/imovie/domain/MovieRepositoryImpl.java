package org.har01d.imovie.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MovieRepositoryImpl implements MovieRepository {

    private static AtomicInteger idGenerator = new AtomicInteger();

    private Map<Integer, Movie> movies = new HashMap<>();

    @Override
    public Movie get(Integer id) {
        return movies.get(id);
    }

    @Override
    public Movie save(Movie movie) {
        Integer id = movie.getId();
        if (id == null) {
            id = idGenerator.incrementAndGet();
            movie.setId(id);
        }

        movies.put(id, movie);
        return movie;
    }
}
