package org.har01d.imovie.service;

import java.util.Set;
import org.har01d.imovie.domain.Category;
import org.har01d.imovie.domain.Language;
import org.har01d.imovie.domain.Person;
import org.har01d.imovie.domain.Region;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.domain.Tag;

public interface MovieService {

    Set<Person> getPersons(Set<String> names);

    Set<Category> getCategories(Set<String> names);

    Set<Language> getLanguages(Set<String> names);

    Set<Region> getRegions(Set<String> names);

    Set<Tag> getTags(Set<String> names);

    Resource saveResource(String uri, String title);

    Resource saveResource(String uri, String original, String title);
}
