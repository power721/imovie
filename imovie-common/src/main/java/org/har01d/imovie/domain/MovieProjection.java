package org.har01d.imovie.domain;

import java.util.Set;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "detail", types = {Movie.class})
public interface MovieProjection {
    public Integer getId();

    public String getTitle();

    public String getName();

    public Integer getYear();

    public Set<Region> getRegions();

    public String getSynopsis();

    public String getCover();

    public Set<Resource> getResources();

    public Set<Category> getCategories();

    public Set<String> getAliases();

    public Set<Person> getDirectors();

    public Set<Person> getEditors();

    public Set<Person> getActors();

    public Set<Language> getLanguages();

    public Set<String> getSnapshots();

    public String getReleaseDate();

    public String getRunningTime();

    public String getImdbUrl();

    public String getImdbScore();

    public String getDbUrl();

    public String getDbScore();
}
