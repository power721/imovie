package org.har01d.imovie.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
import org.har01d.imovie.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovieServiceImpl implements MovieService {

    private static final Logger logger = LoggerFactory.getLogger(MovieService.class);

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
    @Transactional
    public void fixDuplicateMovies() {
        Set<String> dbUrls = movieRepository.findDuplicated();
        logger.info("find {} duplicated movies", dbUrls.size());
        int i = 1;
        for (String dbUrl : dbUrls) {
            List<Movie> movies = movieRepository.findByDbUrl(dbUrl);
            List<Movie> deleted = new ArrayList<>();
            logger.info("handle {}/{}: {}", i++, dbUrls.size(), dbUrl);
            Movie m = null;
            for (Movie movie : movies) {
                if (m == null) {
                    m = movie;
                } else {
                    m.getRes().addAll(movie.getRes());
                    movie.setActors(Collections.emptySet());
                    movie.setEditors(Collections.emptySet());
                    movie.setDirectors(Collections.emptySet());
                    movie.setCategories(Collections.emptySet());
                    movie.setLanguages(Collections.emptySet());
                    movie.setResources(Collections.emptySet());
                    deleted.add(movie);
                }
            }

            if (m == null) {
                continue;
            }

            m.setSize(m.getRes().size());
            movieRepository.save(m);
            logger.info("update movie {}: {} Resources: {}", m.getId(), m.getName(), m.getRes().size());

            for (Movie movie : deleted) {
                movieRepository.delete(movie);
                logger.info("delete movie {}: {}", movie.getId(), movie.getName());
            }
        }
    }

    @Override
    public void fixDuplicateResources(int offset, int limit) {
        List<Resource> resources = resourceRepository.findTop(offset, limit);
        logger.info("try to fix {}-{} resources", offset, resources.size());
        int id = 0;
        for (Resource resource : resources) {
            id++;
            List<Resource> all = resourceRepository.findByUri(resource.getUri(), resource.getId());
            if (all.size() <= 1) {
                offset += all.size();
                continue;
            }

            logger.info("{}-handle {}: {}-{}", id, resource.getId(), resource.getTitle(), resource.getUri());
            List<Resource> deleted = new ArrayList<>();
            Resource kept = null;
            for (Resource r : all) {
                if (r.getMovies().isEmpty()) {
                    deleted.add(r);
                } else {
                    if (kept == null) {
                        kept = r;
                    } else {
                        deleted.add(r);
                    }
                }
            }

            if (kept == null) {
                offset++;
                kept = resource;
                deleted.remove(kept);
            }

            logger.info("{}-keep resource {}: {}-{}", id, kept.getId(), kept.getTitle(), kept.getUri());
            for (Resource r : deleted) {
                delete(r);
            }
        }

        logger.info("next offset: {}", offset);
    }

    @Override
    @Transactional
    public void delete(Resource resource) {
        for (Movie movie : resource.getMovies()) {
            movie.getRes().remove(resource);
            movie.setSize(movie.getRes().size());
            movieRepository.save(movie);
            logger.info("update movie {}: {} Resources: {}", movie.getId(), movie.getName(),
                movie.getRes().size());
        }
        logger.info("delete resource {}: {}-{}", resource.getId(), resource.getTitle(), resource.getUri());
        resourceRepository.delete(resource);
    }

    @Override
    public Integer getYear(String yearStr) {
        return TextUtils.getYear(yearStr);
    }

    @Override
    public Set<Person> getPersons(Set<String> names) {
        Set<Person> persons = new HashSet<>();
        for (String name : names) {
            if (name.endsWith(")")) {
                int index = name.indexOf('(');
                if (index > 0) {
                    name = name.substring(0, index);
                }
            }
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
                if ("愛情 Romance".equals(name)) {
                    name = "愛情";
                } else if ("Western".equals(name)) {
                    name = "西部";
                } else if ("傳記 Biography".equals(name)) {
                    name = "传记";
                } else if ("喜劇 Comedy".equals(name) || "Comedy".equals(name)) {
                    name = "喜剧";
                } else if ("Family".equals(name)) {
                    name = "家庭";
                } else if ("動作 Action".equals(name)) {
                    name = "动作";
                } else if ("動畫 Animation".equals(name)) {
                    name = "动画";
                } else if ("劇情 Drama".equals(name) || "Drama".equals(name)) {
                    name = "剧情";
                } else if ("Talk-Show".equals(name)) {
                    name = "脱口秀";
                } else if ("悬念".equals(name)) {
                    name = "悬疑";
                } else if ("紀錄片 Documentary".equals(name)) {
                    name = "纪录片";
                } else if ("惊栗".equals(name) || "驚悚 Thriller".equals(name)) {
                    name = "惊悚";
                } else if ("Adult".equals(name)) {
                    name = "情色";
                } else if ("音樂 Music".equals(name)) {
                    name = "音乐";
                } else if ("Short".equals(name)) {
                    name = "短片";
                } else if ("戰爭 War".equals(name)) {
                    name = "战争";
                } else if ("懸疑 Mystery".equals(name)) {
                    name = "悬疑";
                } else if ("记录".equals(name)) {
                    name = "纪录片";
                } else if ("鬼怪".equals(name)) {
                    name = "恐怖";
                }
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
                if ("普通话".equals(name)) {
                    name = "汉语普通话";
                } else if ("国语".equals(name) || "國語".equals(name)) {
                    name = "汉语普通话";
                } else if ("普通话/国语".equals(name)) {
                    name = "汉语普通话";
                } else if ("中文".equals(name)) {
                    name = "汉语普通话";
                } else if ("汉语".equals(name)) {
                    name = "汉语普通话";
                } else if ("英文".equals(name) || "英語".equals(name) || "English".equals(name)) {
                    name = "英语";
                }
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
        return save(movie, true);
    }

    @Override
    public Movie save(Movie movie, boolean updateTime) {
        if (movie.getId() == null) {
            logger.debug("create movie {}:{}", movie.getId(), movie.getName());
        } else {
            logger.debug("update movie {}:{}", movie.getId(), movie.getName());
        }
        if (updateTime) {
            movie.setUpdatedTime(new Date());
        }
        movie.setSize(movie.getRes().size());
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
    public Config save(Config config) {
        return configRepository.save(config);

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
    @Transactional
    public List<Movie> findByName(String name) {
        return movieRepository.findByNameStartsWith(name);
    }

    @Override
    public Movie searchByName(Movie movie) {
        if (movie.getName() == null) {
            return null;
        }

        List<Movie> movies = movieRepository.findByNameContains(movie.getName());
        return findBestMatchedMovie(movies, movie);
    }

    @Override
    @Transactional
    public Movie findByDbUrl(String url) {
        return movieRepository.findFirstByDbUrl(url);
    }

    @Override
    @Transactional
    public Movie findByImdb(String url) {
        return movieRepository.findFirstByImdbUrl(url);
    }

    @Override
    @Transactional
    public Movie findById(Integer id) {
        return movieRepository.findOne(id);
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
    public void deleteConfig(String name) {
        Config config = configRepository.findOne(name);
        if (config != null) {
            configRepository.delete(config);
        }
    }

    @Override
    public Resource saveResource(String uri, String title) {
        return saveResource(null, uri, title);
    }

    @Override
    public Resource saveResource(String uri, String original, String title) {
        Resource resource;
        if (uri != null) {
            resource = resourceRepository.findFirstByUri(uri);
            if (resource != null) {
                resource.setOriginal(original);
                return resource;
            }
        } else {
            resource = resourceRepository.findFirstByUri(original);
            if (resource != null) {
                return resource;
            }
        }

        title = TextUtils.truncate(title, 200);
        if (uri == null || uri.equals(original)) {
            resource = new Resource(original, title);
        } else {
            resource = new Resource(uri, original, title);
        }
        resource.setNew(true);
        resourceRepository.save(resource);
        return resource;
    }

    @Override
    public Movie updateMovie(String url, Movie movie) {
        Movie m = findByDbUrl(url);
        if (m != null) {
            m.setEpisode(movie.getEpisode());
            m.setDbScore(movie.getDbScore());
            m.setAliases(movie.getAliases());
            m.setDirectors(movie.getDirectors());
            m.setEditors(movie.getEditors());
            m.setActors(movie.getActors());
            m.setSynopsis(movie.getSynopsis());
            m.setSize(m.getRes().size());
            return movieRepository.save(m);
        }
        movie.setSize(movie.getRes().size());
        return movieRepository.save(movie);
    }

    @Override
    @Transactional
    public Movie updateMovie(Movie movie) {
        return movieRepository.save(movie);
    }

    @Override
    public Resource findResource(String uri) {
        Resource resource = resourceRepository.findFirstByUri(uri);
        if (resource != null) {
            return resource;
        }

        resource = resourceRepository.findFirstByOriginal(uri);
        return resource;
    }

    @Override
    public Resource findResourceByOriginal(String uri) {
        return resourceRepository.findFirstByOriginal(uri);
    }

    @Override
    public Event publishEvent(String source, String message) {
        return eventRepository.save(new Event(TextUtils.truncate(source, 255), message));
    }

    @Override
    public Movie findBestMatchedMovie(List<Movie> movies, Movie movie) {
        Movie best = null;
        int maxMatch = 0;
        for (Movie m : movies) {
            int match = 0;
            if (movie.getName().equals(m.getName())) {
                match += 15;
            } else if (m.getName().startsWith(movie.getName())) {
                match += 10;
            }

            if (m.getAliases() != null) {
                for (String name : m.getAliases()) {
                    if (name.equals(movie.getName())) {
                        match += 10;
                        break;
                    }
                }
            }

            if (movie.getYear() != null) {
                if (movie.getYear().equals(m.getYear())) {
                    match += 10;
                } else {
                    match -= 5;
                }
            }

            if (movie.getCategories() != null && !movie.getCategories().isEmpty() && m.getCategories() != null && !m
                .getCategories().isEmpty()) {
                if (m.getCategories().containsAll(movie.getCategories())) {
                    match += 5;
                } else if (movie.getCategories().containsAll(m.getCategories())) {
                    match += 5;
                }
            }

            if (movie.getRegions() != null && !movie.getRegions().isEmpty() && m.getRegions() != null) {
                if (m.getRegions().containsAll(movie.getRegions())) {
                    match += 5;
                }
            }

            if (movie.getLanguages() != null && !movie.getLanguages().isEmpty() && m.getLanguages() != null) {
                if (m.getLanguages().containsAll(movie.getLanguages())) {
                    match += 5;
                }
            }

            if (movie.getAliases() != null && !movie.getAliases().isEmpty() && m.getAliases() != null) {
                if (m.getAliases().containsAll(movie.getAliases())) {
                    match += 10;
                } else if (movie.getAliases().containsAll(m.getAliases())) {
                    match += 5;
                }
            }

            if (movie.getDirectors() != null && !movie.getDirectors().isEmpty() && m.getDirectors() != null) {
                if (containsAll(m.getDirectors(), movie.getDirectors())) {
                    match += 10;
                }
            }

            if (movie.getEditors() != null && !movie.getEditors().isEmpty() && m.getEditors() != null) {
                if (containsAll(m.getEditors(), movie.getEditors())) {
                    match += 10;
                }
            }

            if (movie.getActors() != null && !movie.getActors().isEmpty() && m.getActors() != null) {
                if (containsAll(m.getActors(), movie.getActors())) {
                    match += 10;
                }
            }

            if (movie.getReleaseDate() != null && m.getReleaseDate() != null) {
                if (getDates(m.getReleaseDate()).containsAll(getDates(movie.getReleaseDate()))) {
                    match += 10;
                }
            }

            if (movie.getRunningTime() != null && m.getRunningTime() != null) {
                if (m.getRunningTime().equals(movie.getRunningTime())) {
                    match += 10;
                }
            }

            if (movie.getImdbUrl() != null && m.getImdbUrl() != null) {
                if (m.getImdbUrl().equals(movie.getImdbUrl())) {
                    match += 15;
                }
            }

            if (movie.getWebsite() != null && m.getWebsite() != null) {
                if (m.getWebsite().equals(movie.getWebsite())) {
                    match += 5;
                }
            }

            if (movie.getSynopsis() != null && !movie.getSynopsis().isEmpty() && m.getSynopsis() != null) {
                if (m.getSynopsis().replaceAll("　", "").trim().equals(movie.getSynopsis().trim())) {
                    match += 20;
                } else if (m.getSynopsis().replaceAll("　", "").trim().contains(movie.getSynopsis().trim())) {
                    match += 10;
                }
            }

            if (movie.getEpisode() != null && m.getEpisode() != null && movie.getEpisode() > 0) {
                if (Objects.equals(m.getEpisode(), movie.getEpisode())) {
                    match += 10;
                }
            }

            if (match > 20 && match > maxMatch) {
                maxMatch = match;
                best = m;
            }
        }

        if (best != null) {
            logger.info("find best matched movie {} for {}, match: {}", best.getTitle(), movie.getName(), maxMatch);
        }
        return best;
    }

    private boolean containsAll(Set<Person> people1, Set<Person> people2) {
        Set<String> names1 = people1.stream().map(e -> e.getName().replace("·", " ")).collect(Collectors.toSet());
        Set<String> names2 = people2.stream().map(e -> e.getName().replace("·", " ")).collect(Collectors.toSet());
        return names1.containsAll(names2);
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
