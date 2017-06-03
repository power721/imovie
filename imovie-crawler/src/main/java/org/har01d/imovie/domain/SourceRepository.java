package org.har01d.imovie.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRepository extends JpaRepository<Source, Integer> {
    Source findFirstByUri(String uri);
}
