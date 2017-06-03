package org.har01d.imovie.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(excerptProjection = MovieExcerpt.class)
public interface MovieRepository extends JpaRepository<Movie, Integer> {

    @RestResource(exported = false)
    List<Movie> findByNameStartsWith(String name);

    @RestResource(exported = false)
    Movie findFirstByDbUrl(String dbUrl);

    @RestResource(exported = false)
    Movie findFirstByImdbUrl(String imdbUrl);

    @RestResource(path = "by-name", rel = "by-name")
    Page<Movie> findByNameContaining(@Param("name") String name, Pageable pageable);

    @RestResource(path = "by-category", rel = "by-category")
    Page<Movie> findByCategories_Id(@Param("id") Integer categoryId, Pageable pageable);

    @RestResource(path = "by-language", rel = "by-language")
    Page<Movie> findByLanguages_Id(@Param("id") Integer languageId, Pageable pageable);

    @RestResource(path = "by-region", rel = "by-region")
    Page<Movie> findByRegions_Id(@Param("id") Integer regionId, Pageable pageable);

//    @RestResource(path = "by-person", rel = "by-person")
//    @Query("SELECT * from ")
//    Page<Movie> findByPerson_Id(@Param("id") Integer personId, Pageable pageable);
}
