package org.har01d.imovie.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(excerptProjection = MovieProjection.class)
public interface MovieRepository extends JpaRepository<Movie, Integer> {

    List<Movie> findByDbUrlContaining(String search);
    Movie findFirstByDbUrl(String dbUrl);
}
