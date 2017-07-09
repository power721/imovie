package org.har01d.imovie.web.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.har01d.imovie.web.domain.Movie;
import org.har01d.imovie.web.domain.MovieRepository;
import org.har01d.imovie.web.domain.Resource;
import org.har01d.imovie.web.domain.ResourceRepository;
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

    @Transactional
    @Override
    public Set<Integer> transferResources(Set<Integer> resourceIds, Integer movieId) {
        List<Resource> resources = new ArrayList<>();
        for (Integer id : resourceIds) {
            Resource resource = resourceRepository.getOne(id);
            if (resource != null) {
                for (Movie movie : resource.getMovies()) {
                    movie.getRes().remove(resource);
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
                int size = movie.getResourcesSize();
                movie.getRes().addAll(resources);
                size = movie.getResourcesSize() - size;
                movieRepository.save(movie);
                log.info("update movie {}: {} Resources: +{}/{}", movie.getId(), movie.getName(), size,
                    movie.getRes().size());
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

}
