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

    @RestResource(path = "search", rel = "search")
    Page<Movie> findByNameContainingAndCategories_Name(@Param("name") String name, @Param("category") String category, Pageable pageable);

    @RestResource(path = "by-category", rel = "by-category")
    Page<Movie> findByCategories_Name(@Param("category") String category, Pageable pageable);

    @RestResource(path = "by-language", rel = "by-language")
    Page<Movie> findByLanguages_Name(@Param("language") String language, Pageable pageable);

    @RestResource(path = "by-region", rel = "by-region")
    Page<Movie> findByRegions_Name(@Param("region") String region, Pageable pageable);

    @RestResource(path = "by-imdb", rel = "by-imdb")
    Page<Movie> findByImdbUrlContaining(@Param("name") String imdb, Pageable pageable);


    @RestResource(path = "by-movie", rel = "by-movie")
    Page<Movie> findByEpisodeEquals(@Param("episode") int episode, Pageable pageable);

    @RestResource(path = "by-movie-category", rel = "by-movie-category")
    Page<Movie> findByEpisodeEqualsAndCategories_Name(@Param("episode") int episode, @Param("category") String category, Pageable pageable);

    @RestResource(path = "by-movie-name", rel = "by-movie-name")
    Page<Movie> findByEpisodeEqualsAndNameContaining(@Param("episode") int episode, @Param("name") String name, Pageable pageable);

    @RestResource(path = "search-movie", rel = "search-movie")
    Page<Movie> findByEpisodeEqualsAndNameContainingAndCategories_Name(@Param("episode") int episode, @Param("name") String name, @Param("category") String category, Pageable pageable);

    @RestResource(path = "by-movie-imdb", rel = "by-movie-imdb")
    Page<Movie> findByEpisodeEqualsAndImdbUrlContaining(@Param("episode") int episode, @Param("name") String imdb, Pageable pageable);


    @RestResource(path = "by-episode", rel = "by-episode")
    Page<Movie> findByEpisodeGreaterThan(@Param("episode") int episode, Pageable pageable);

    @RestResource(path = "by-episode-category", rel = "by-episode-category")
    Page<Movie> findByEpisodeGreaterThanAndCategories_Name(@Param("episode") int episode, @Param("category") String category, Pageable pageable);

    @RestResource(path = "by-episode-name", rel = "by-episode-name")
    Page<Movie> findByEpisodeGreaterThanAndNameContaining(@Param("episode") int episode, @Param("name") String name, Pageable pageable);

    @RestResource(path = "search-episode", rel = "search-episode")
    Page<Movie> findByEpisodeGreaterThanAndNameContainingAndCategories_Name(@Param("episode") int episode, @Param("name") String name, @Param("category") String category, Pageable pageable);

    @RestResource(path = "by-episode-imdb", rel = "by-episode-imdb")
    Page<Movie> findByEpisodeGreaterThanAndImdbUrlContaining(@Param("episode") int episode, @Param("name") String imdb, Pageable pageable);

}
