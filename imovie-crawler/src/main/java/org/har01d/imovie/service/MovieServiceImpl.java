package org.har01d.imovie.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.har01d.imovie.domain.Category;
import org.har01d.imovie.domain.CategoryRepository;
import org.har01d.imovie.domain.Config;
import org.har01d.imovie.domain.ConfigRepository;
import org.har01d.imovie.domain.Event;
import org.har01d.imovie.domain.EventRepository;
import org.har01d.imovie.domain.Explorer;
import org.har01d.imovie.domain.ExplorerRepository;
import org.har01d.imovie.domain.Language;
import org.har01d.imovie.domain.LanguageRepository;
import org.har01d.imovie.domain.Movie;
import org.har01d.imovie.domain.MovieRepository;
import org.har01d.imovie.domain.Person;
import org.har01d.imovie.domain.PersonRepository;
import org.har01d.imovie.domain.Region;
import org.har01d.imovie.domain.RegionRepository;
import org.har01d.imovie.domain.Resource;
import org.har01d.imovie.domain.ResourceRepository;
import org.har01d.imovie.domain.Source;
import org.har01d.imovie.domain.SourceRepository;
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

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private ExplorerRepository explorerRepository;

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
    public Movie save(Movie movie) {
        return movieRepository.save(movie);
    }

    @Override
    public Source save(Source source) {
        return sourceRepository.save(source);
    }

    @Override
    public Resource save(Resource resource) {
        return resourceRepository.save(resource);
    }

    @Override
    public Explorer save(Explorer explorer) {
        return explorerRepository.save(explorer);
    }

    @Override
    public void delete(Explorer explorer) {
        explorerRepository.delete(explorer);
    }

    @Override
    public List<Explorer> findExplorers(String type) {
        return explorerRepository.findByType(type);
    }

    @Override
    public Movie find(String url) {
        return movieRepository.findFirstByDbUrl(url);
    }

    @Override
    public Source findSource(String url) {
        return sourceRepository.findFirstByUri(url);
    }

    @Override
    public Config saveConfig(String name, String value) {
        return configRepository.save(new Config(name, value));
    }

    @Override
    public Config getConfig(String name) {
        return configRepository.findOne(name);
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

    @Override
    public Event publishEvent(String source, String message) {
        return eventRepository.save(new Event(source, message));
    }

}
