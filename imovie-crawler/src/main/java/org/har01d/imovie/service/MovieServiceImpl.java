package org.har01d.imovie.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.har01d.imovie.domain.Category;
import org.har01d.imovie.domain.CategoryRepository;
import org.har01d.imovie.domain.Language;
import org.har01d.imovie.domain.LanguageRepository;
import org.har01d.imovie.domain.MovieRepository;
import org.har01d.imovie.domain.Person;
import org.har01d.imovie.domain.PersonRepository;
import org.har01d.imovie.domain.Region;
import org.har01d.imovie.domain.RegionRepository;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.domain.ResourceRepository;
import org.har01d.imovie.domain.Tag;
import org.har01d.imovie.domain.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MovieServiceImpl implements MovieService {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private LanguageRepository languageRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private TagRepository tagRepository;

    @Override
    public Set<Person> getPersons(Set<String> names) {
        Set<Person> persons = new HashSet<>();
        for (String name : names) {
            Optional<Person> person = personRepository.findFirstByName(name);
            if (person.isPresent()) {
                persons.add(person.get());
            } else {
                Person p = new Person(name);
                personRepository.save(p);
                persons.add(p);
            }
        }
        return persons;
    }

    @Override
    public Set<Category> getCategories(Set<String> names) {
        Set<Category> categories = new HashSet<>();
        for (String name : names) {
            Optional<Category> category = categoryRepository.findFirstByName(name);
            if (category.isPresent()) {
                categories.add(category.get());
            } else {
                Category c = new Category(name);
                categoryRepository.save(c);
                categories.add(c);
            }
        }
        return categories;
    }

    @Override
    public Set<Language> getLanguages(Set<String> names) {
        Set<Language> languages = new HashSet<>();
        for (String name : names) {
            Optional<Language> language = languageRepository.findFirstByName(name);
            if (language.isPresent()) {
                languages.add(language.get());
            } else {
                Language l = new Language(name);
                languageRepository.save(l);
                languages.add(l);
            }
        }
        return languages;
    }

    @Override
    public Set<Region> getRegions(Set<String> names) {
        Set<Region> regions = new HashSet<>();
        for (String name : names) {
            Optional<Region> region = regionRepository.findFirstByName(name);
            if (region.isPresent()) {
                regions.add(region.get());
            } else {
                Region r = new Region(name);
                regionRepository.save(r);
                regions.add(r);
            }
        }
        return regions;
    }

    @Override
    public Set<Tag> getTags(Set<String> names) {
        Set<Tag> tags = new HashSet<>();
        for (String name : names) {
            Optional<Tag> tag = tagRepository.findFirstByName(name);
            if (tag.isPresent()) {
                tags.add(tag.get());
            } else {
                Tag t = new Tag(name);
                tagRepository.save(t);
                tags.add(t);
            }
        }
        return tags;
    }

    @Override
    public Resource saveResource(String uri, String title) {
        return saveResource(null, uri, title);
    }

    @Override
    public Resource saveResource(String uri, String original, String title) {
        Resource resource = resourceRepository.findFirstByUri(uri);
        if (resource != null) {
            return resource;
        }

        if (uri == null) {
            resource = new Resource(original, title);
        } else {
            resource = new Resource(uri, original, title);
        }
        resourceRepository.save(resource);
        return resource;
    }

}
