package org.har01d.imovie.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ResourceRepository extends JpaRepository<Resource, Integer> {

    Resource findFirstByUri(String uri);

    @Query(value = "SELECT r.* FROM resource r WHERE uri = ?1 AND id <= ?2", nativeQuery = true)
    List<Resource> findByUri(String uri, int id);

    Resource findFirstByOriginal(String uri);

    List<Resource> findByUriStartingWith(String uri);

    @Query(value = "SELECT r.* FROM resource r ORDER BY r.id DESC LIMIT ?2 OFFSET ?1", nativeQuery = true)
    List<Resource> findTop(int offset, int limit);
}
