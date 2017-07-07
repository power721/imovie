package org.har01d.imovie.web.service;

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

}
