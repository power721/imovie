package org.har01d.imovie.web.domain;

import java.util.Set;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "detail", types = {Movie.class})
public interface MovieProjection {

    Integer getId();

    String getTitle();

    String getName();

    Integer getYear();

    Set<Region> getRegions();

    String getSynopsis();

    String getCover();

    Set<Resource> getResources();

    Set<Category> getCategories();

    Set<String> getAliases();

    Set<Person> getDirectors();

    Set<Person> getEditors();

    Set<Person> getActors();

    Set<Language> getLanguages();

    Set<String> getSnapshots();

    String getReleaseDate();

    String getRunningTime();

    String getImdbUrl();

    String getImdbScore();

    String getDbUrl();

    String getDbScore();
}
