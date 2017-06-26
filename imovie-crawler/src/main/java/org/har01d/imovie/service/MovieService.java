package org.har01d.imovie.service;

import java.util.List;
import java.util.Set;
import org.har01d.imovie.domain.Category;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.Event;
import org.har01d.imovie.domain.Explorer;
import org.har01d.imovie.domain.Language;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.Person;
import org.har01d.imovie.domain.Region;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.domain.Source;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface MovieService {

    @Transactional
    void fixDuplicateMovies();

    Integer getYear(String yearStr);

    Set<Person> getPersons(Set<String> names);

    Set<Category> getCategories(Set<String> names);

    Set<Language> getLanguages(Set<String> names);

    Set<Region> getRegions(Set<String> names);

    Movie save(Movie movie);

    Source save(Source source);

    Resource save(Resource resource);

    Explorer save(Explorer explorer);

    void delete(Explorer explorer);

    Page<Explorer> findExplorers(String type, Pageable pageable);

    List<Movie> findByName(String name);

    Movie findByDbUrl(String url);

    Movie findByImdb(String url);

    Source findSource(String url);

    Config saveConfig(String name, String value);

    Config getConfig(String name);

    void deleteConfig(Config config);

    Resource saveResource(String uri, String title);

    Resource saveResource(String uri, String original, String title);

    Resource findResource(String uri);

    Event publishEvent(String source, String message);

    Movie findBestMatchedMovie(List<Movie> movies, Movie movie);
}
