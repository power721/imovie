package org.har01d.imovie.web.service;

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.har01d.imovie.web.domain.Movie;
import org.har01d.imovie.web.domain.MovieRepository;
import org.har01d.imovie.web.domain.Resource;
import org.har01d.imovie.web.domain.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MovieServiceImpl implements MovieService {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Override
    public void deleteResource(Integer id) {
        Resource resource = resourceRepository.getOne(id);
        for (Movie movie : resource.getMovies()) {
            movie.getRes().remove(resource);
            movieRepository.save(movie);
            log.info("update movie {}: {} Resources: {}", movie.getId(), movie.getName(), movie.getRes().size());
        }

        resourceRepository.delete(resource);
        log.info("delete resource {}: {}", id, resource.getTitle());
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

}
