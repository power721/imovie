package org.har01d.imovie.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Integer> {
    List<Movie> findByNameStartsWith(String name);

    Movie findFirstByDbUrl(String dbUrl);

    Movie findFirstByImdbUrl(String imdbUrl);
}
