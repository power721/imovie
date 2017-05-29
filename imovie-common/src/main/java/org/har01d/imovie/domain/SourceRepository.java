package org.har01d.imovie.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface SourceRepository extends JpaRepository<Source, Integer> {

    Source findFirstByUri(String uri);
}
