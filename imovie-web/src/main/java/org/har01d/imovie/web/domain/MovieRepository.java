package org.har01d.imovie.web.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(excerptProjection = MovieExcerpt.class)
public interface MovieRepository extends JpaRepository<Movie, Integer> {
    @Override
    @RestResource(exported = false)
    Movie save(Movie entity);

    @Override
    @RestResource(exported = false)
    void delete(Integer id);

    @Override
    @RestResource(exported = false)
    void delete(Movie entity);

    @RestResource(path = "by-name", rel = "by-name")
    Page<Movie> findByNameContaining(@Param("name") String name, Pageable pageable);

    @RestResource(path = "by-category", rel = "by-category")
    Page<Movie> findByCategories_Name(@Param("category") String category, Pageable pageable);

    @RestResource(path = "by-language", rel = "by-language")
    Page<Movie> findByLanguages_Name(@Param("language") String language, Pageable pageable);

    @RestResource(path = "by-region", rel = "by-region")
    Page<Movie> findByRegions_Name(@Param("region") String region, Pageable pageable);
}
