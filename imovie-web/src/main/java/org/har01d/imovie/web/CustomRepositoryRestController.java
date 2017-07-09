package org.har01d.imovie.web;

import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.contains;

import java.util.Collections;
import java.util.Set;
import org.har01d.imovie.web.domain.Category;
import org.har01d.imovie.web.domain.Movie;
import org.har01d.imovie.web.domain.MovieRepository;
import org.har01d.imovie.web.dto.TransferParam;
import org.har01d.imovie.web.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@RepositoryRestController
public class CustomRepositoryRestController {

    @Autowired
    private MovieRepository repository;

    @Autowired
    private MovieService service;

    @DeleteMapping("/resources/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@PathVariable Integer id) {
        service.deleteResource(id);
    }

    @DeleteMapping("/movies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMovie(@PathVariable Integer id) {
        service.deleteMovie(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    @PostMapping("/resources/transfer")
    public Set<Integer> transferResources(@RequestBody TransferParam transferParam) {
        return service.transferResources(transferParam.getResources(), transferParam.getMovieId());
    }

    @ResponseBody
    @GetMapping("/movies/filter")
    public Page<Movie> filterMovies(@RequestParam(defaultValue = "", required = false) String name,
        @RequestParam(defaultValue = "", required = false) String category, Pageable pageable) {
        if (StringUtils.isEmpty(name) && (StringUtils.isEmpty(category) || "all".equals(category))) {
            return repository.findAll(pageable);
        }

        Movie movie = new Movie();
        if (!StringUtils.isEmpty(name)) {
            movie.setName(name);
        }
        if (!StringUtils.isEmpty(category) && !"all".equals(category)) {
            movie.setCategories(Collections.singleton(new Category(category)));
        }
        ExampleMatcher matcher = ExampleMatcher.matching()
            .withMatcher("name", contains().ignoreCase()).withIgnoreNullValues();
        Example<Movie> example = Example.of(movie, matcher);
        return repository.findAll(example, pageable);
    }

}
