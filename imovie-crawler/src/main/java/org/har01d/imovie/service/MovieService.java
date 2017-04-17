package org.har01d.imovie.service;

import java.util.Set;
import org.har01d.imovie.domain.Category;
import org.har01d.imovie.domain.Language;
import org.har01d.imovie.domain.Person;
import org.har01d.imovie.domain.Region;

public interface MovieService {

    Set<Person> getPersons(Set<String> names);

    Set<Category> getCategories(Set<String> names);

    Set<Language> getLanguages(Set<String> names);

    Set<Region> getRegions(Set<String> names);
}
