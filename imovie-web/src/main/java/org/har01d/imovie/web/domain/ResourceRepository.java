package org.har01d.imovie.web.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

public interface ResourceRepository extends JpaRepository<Resource, Integer> {

    @RestResource(path = "search", rel = "search")
    Page<Resource> findByTitleContainingOrUriContaining(@Param("text") String text1, @Param("text") String text2, Pageable pageable);

}
