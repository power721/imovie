package org.har01d.imovie.web.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.har01d.imovie.web.domain.Category;
import org.har01d.imovie.web.domain.CategoryRepository;
import org.har01d.imovie.web.domain.Language;
import org.har01d.imovie.web.domain.LanguageRepository;
import org.har01d.imovie.web.domain.Movie;
import org.har01d.imovie.web.domain.MovieRepository;
import org.har01d.imovie.web.domain.Person;
import org.har01d.imovie.web.domain.PersonRepository;
import org.har01d.imovie.web.domain.Region;
import org.har01d.imovie.web.domain.RegionRepository;
import org.har01d.imovie.web.domain.Resource;
import org.har01d.imovie.web.domain.ResourceRepository;
import org.har01d.imovie.web.dto.ResourceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MovieServiceImpl implements MovieService {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LanguageRepository languageRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private DouBanParser douBanParser;

    @Override
    public void deleteResource(Integer id) {
        Resource resource = resourceRepository.getOne(id);
        for (Movie movie : resource.getMovies()) {
            movie.removeResource(resource);
            movieRepository.save(movie);
            log.info("update movie {}: {} Resources: {}", movie.getId(), movie.getName(), movie.getRes().size());
        }

        resourceRepository.delete(resource);
        log.info("delete resource {}: {}", id, resource.getTitle());
    }

    @Override
    @Transactional
    public Resource addResource(Integer id, @Valid ResourceDTO resourceDTO) {
        Resource resource = resourceRepository.findFirstByUri(resourceDTO.getUri());
        if (resource == null) {
            resource = resourceRepository.save(new Resource(resourceDTO.getUri(), resourceDTO.getTitle()));
        }

        Movie movie = movieRepository.getOne(id);
        if (movie != null) {
            movie.addResource(resource);
            movieRepository.save(movie);
            log.info("update movie {}: add resource {}:{}", movie.getId(), resource.getId(), resource.getUri());
        }
        return resource;
    }

    @Transactional
    @Override
    public Set<Integer> transferResources(Set<Integer> resourceIds, Integer movieId) {
        List<Resource> resources = new ArrayList<>();
        for (Integer id : resourceIds) {
            Resource resource = resourceRepository.getOne(id);
            if (resource != null) {
                for (Movie movie : resource.getMovies()) {
                    movie.removeResource(resource);
                    movieRepository.save(movie);
                    log.info("update movie {}: {} Resources: {}", movie.getId(), movie.getName(),
                        movie.getRes().size());
                }
                resources.add(resource);
            }
        }

        if (movieId != null && movieId != 0) {
            Movie movie = movieRepository.getOne(movieId);
            if (movie != null) {
                movie.addResources(resources);
                movieRepository.save(movie);
                log.info("update movie {}: {} Resources: +{}/{}", movie.getId(), movie.getName(),
                    movie.getNewResources(), movie.getRes().size());
            }
        }
        return resources.stream().map(Resource::getId).collect(Collectors.toSet());
    }

    @Override
    public void deleteMovie(Integer id) {
        Movie movie = movieRepository.getOne(id);
        movie.setActors(Collections.emptySet());
        movie.setEditors(Collections.emptySet());
        movie.setDirectors(Collections.emptySet());
        movie.setCategories(Collections.emptySet());
        movie.setLanguages(Collections.emptySet());
        movie.setResources(Collections.emptyList());
        movieRepository.delete(movie);
        log.info("delete movie {}: {}", movie.getId(), movie.getName());
    }

    @Override
    public void refreshMovie(Integer id) throws IOException {
        Movie movie = movieRepository.getOne(id);
        if (movie != null && StringUtils.isNotEmpty(movie.getDbUrl())) {
            movie = douBanParser.parse(movie);
            movieRepository.save(movie);
        }
    }

    @Override
    public Set<Person> getPeople(Set<String> names) {
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

}
