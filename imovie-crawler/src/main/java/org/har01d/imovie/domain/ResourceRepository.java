package org.har01d.imovie.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Integer> {
    Resource findFirstByUri(String uri);

    Resource findFirstByOriginal(String uri);

    List<Resource> findByUriStartingWith(String uri);
}
