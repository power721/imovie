package org.har01d.imovie.web;

import org.har01d.imovie.web.domain.Movie;
import org.har01d.imovie.web.domain.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.web.bind.annotation.PostMapping;

@RepositoryRestController
public class EpisodeController {

    @Autowired
    private MovieRepository repository;

    @PostMapping("/episodes")
    Page<Movie> getMovies(String name, Pageable pageable) {
        return repository.findByNameContaining(name, pageable);
    }

}
