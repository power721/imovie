package org.har01d.imovie.service;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MovieServiceImpl implements MovieService {

    private static final Logger logger = LoggerFactory.getLogger(MovieService.class);

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})-\\d{2}-\\d{2}");
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\s*(\\d{4})\\D*");
    private Pattern DATE1 = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");
    private Pattern DATE2 = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})");
    private Pattern DATE3 = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})日");

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
    private SourceRepository sourceRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private ExplorerRepository explorerRepository;

    @Override
    public Integer getYear(String yearStr) {
        if (yearStr == null) {
            return null;
        }

        Matcher matcher = DATE_PATTERN.matcher(yearStr);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }

        matcher = YEAR_PATTERN.matcher(yearStr);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }
        return null;
    }

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
    public Movie save(Movie movie) {
        movie.setUpdatedTime(new Date());
        return movieRepository.save(movie);
    }

    @Override
    public Source save(Source source) {
        source.setUpdatedTime(new Date());
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
    public Page<Explorer> findExplorers(String type, Pageable pageable) {
        return explorerRepository.findByType(type, pageable);
    }

    @Override
    public List<Movie> findByName(String name) {
        return movieRepository.findByNameStartsWith(name);
    }

    @Override
    public Movie findByDbUrl(String url) {
        return movieRepository.findFirstByDbUrl(url);
    }

    @Override
    public Movie findByImdb(String url) {
        return movieRepository.findFirstByImdbUrl(url);
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
    public void deleteConfig(Config config) {
        configRepository.delete(config);
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

        resource = resourceRepository.findFirstByOriginal(original);
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

    @Override
    public Movie findBestMatchedMovie(List<Movie> movies, Movie movie) {
        Movie best = null;
        int maxMatch = 0;
        for (Movie m : movies) {
            int match = 0;
            if (movie.getName().equals(m.getName())) {
                match += 2;
            } else if (m.getName().startsWith(movie.getName())) {
                match++;
            }

            if (m.getAliases() != null) {
                for (String name : m.getAliases()) {
                    if (name.equals(movie.getName())) {
                        match++;
                        break;
                    }
                }
            }

            if (movie.getYear() != null) {
                if (movie.getYear().equals(m.getYear())) {
                    match++;
                }
            }

            if (movie.getCategories() != null && !movie.getCategories().isEmpty() && m.getCategories() != null) {
                if (m.getCategories().containsAll(movie.getCategories())) {
                    match++;
                } else if (movie.getCategories().containsAll(m.getCategories())) {
                    match++;
                }
            }

            if (movie.getRegions() != null && !movie.getRegions().isEmpty() && m.getRegions() != null) {
                if (m.getRegions().containsAll(movie.getRegions())) {
                    match++;
                }
            }

            if (movie.getLanguages() != null && !movie.getLanguages().isEmpty() && m.getLanguages() != null) {
                if (m.getLanguages().containsAll(movie.getLanguages())) {
                    match++;
                }
            }

            if (movie.getAliases() != null && !movie.getAliases().isEmpty() && m.getAliases() != null) {
                if (m.getAliases().containsAll(movie.getAliases())) {
                    match++;
                }
            }

            if (movie.getDirectors() != null && !movie.getDirectors().isEmpty() && m.getDirectors() != null) {
                if (m.getDirectors().containsAll(movie.getDirectors())) {
                    match++;
                }
            }

            if (movie.getEditors() != null && !movie.getEditors().isEmpty() && m.getEditors() != null) {
                if (m.getEditors().containsAll(movie.getEditors())) {
                    match++;
                }
            }

            if (movie.getActors() != null && !movie.getActors().isEmpty() && m.getActors() != null) {
                if (m.getActors().containsAll(movie.getActors())) {
                    match++;
                }
            }

            if (movie.getReleaseDate() != null && m.getReleaseDate() != null) {
                if (getDates(m.getReleaseDate()).containsAll(getDates(movie.getReleaseDate()))) {
                    match++;
                }
            }

            if (movie.getRunningTime() != null && m.getRunningTime() != null) {
                if (m.getRunningTime().equals(movie.getRunningTime())) {
                    match++;
                }
            }

            if (movie.getImdbUrl() != null && m.getImdbUrl() != null) {
                if (m.getImdbUrl().equals(movie.getImdbUrl())) {
                    match++;
                }
            }

            if (movie.getSynopsis() != null && !movie.getSynopsis().isEmpty() && m.getSynopsis() != null) {
                if (m.getSynopsis().equals(movie.getSynopsis())) {
                    match += 2;
                } else if (m.getSynopsis().contains(movie.getSynopsis())) {
                    match++;
                }
            }

            if (movie.getEpisode() != 0 && m.getEpisode() != 0) {
                if (m.getEpisode() == movie.getEpisode()) {
                    match++;
                }
            }

            if (match > 2 && match > maxMatch) {
                maxMatch = match;
                best = m;
            }
        }
        return best;
    }

    private Set<String> getDates(String text) {
        Set<String> dates = new HashSet<>();
        Matcher m = DATE1.matcher(text);
        while (m.find()) {
            dates.add(m.group(1) + "-" + m.group(2) + "-" + m.group(3));
        }

        m = DATE2.matcher(text);
        while (m.find()) {
            dates.add(m.group(1) + "-" + m.group(2) + "-" + m.group(3));
        }

        m = DATE3.matcher(text);
        while (m.find()) {
            dates.add(m.group(1) + "-" + m.group(2) + "-" + m.group(3));
        }

        if (dates.isEmpty()) {
            dates.add(text);
        }
        return dates;
    }

}
