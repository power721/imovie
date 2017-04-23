package org.har01d.imovie.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(excerptProjection = MovieProjection.class)
public interface MovieRepository extends JpaRepository<Movie, Integer> {
    Movie findFirstByDbUrl(String dbUrl);

    Movie findFirstByImdbUrl(String imdbUrl);
}
