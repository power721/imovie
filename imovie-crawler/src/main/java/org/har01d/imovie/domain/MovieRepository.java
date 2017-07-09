package org.har01d.imovie.domain;

import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MovieRepository extends JpaRepository<Movie, Integer> {

    List<Movie> findByNameStartsWith(String name);

    Movie findFirstByDbUrl(String dbUrl);

    List<Movie> findByDbUrl(String dbUrl);

    @Query(value = "SELECT m.db_url FROM movie m INNER JOIN (SELECT db_url FROM movie GROUP BY db_url HAVING COUNT(db_url) > 1) dup ON m.db_url = dup.db_url", nativeQuery = true)
    Set<String> findDuplicated();

    Movie findFirstByImdbUrl(String imdbUrl);
}
