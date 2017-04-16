package org.har01d.imovie.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Integer> {

    Optional<Resource> findFirstByUri(String uri);
}
