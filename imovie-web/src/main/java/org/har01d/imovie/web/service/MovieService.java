package org.har01d.imovie.web.service;

import java.io.IOException;
import java.util.Set;
import javax.validation.Valid;
import org.har01d.imovie.web.domain.Category;
import org.har01d.imovie.web.domain.Language;
import org.har01d.imovie.web.domain.Person;
import org.har01d.imovie.web.domain.Region;
import org.har01d.imovie.web.domain.Resource;
import org.har01d.imovie.web.dto.ResourceDTO;

public interface MovieService {

    void deleteResource(Integer id);

    Resource addResource(Integer id, @Valid ResourceDTO resourceDTO);

    Set<Integer> transferResources(Set<Integer> resourceIds, Integer movieId);

    void deleteMovie(Integer id);

    void refreshMovie(Integer id) throws IOException;

    Set<Person> getPeople(Set<String> names);

    Set<Category> getCategories(Set<String> names);

    Set<Language> getLanguages(Set<String> names);

    Set<Region> getRegions(Set<String> names);
}
