package org.har01d.imovie.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Integer> {

    Optional<Movie> findFirstBySource(String source);
}
