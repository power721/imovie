package org.har01d.imovie.web;

import static org.springframework.data.rest.webmvc.ControllerUtils.EMPTY_RESOURCE_LIST;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.har01d.imovie.web.domain.Movie;
import org.har01d.imovie.web.domain.MovieRepository;
import org.har01d.imovie.web.domain.Person;
import org.har01d.imovie.web.domain.PersonRepository;
import org.har01d.imovie.web.dto.TransferParam;
import org.har01d.imovie.web.qsl.CustomRsqlVisitor;
import org.har01d.imovie.web.qsl.RsqlSearchOperation;
import org.har01d.imovie.web.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.support.DefaultedPageable;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resources;
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

@ResponseBody
@RepositoryRestController
public class CustomRepositoryRestController {

    private final PagedResourcesAssembler<Object> pagedResourcesAssembler;

    @Autowired
    private RepositoryEntityLinks entityLinks;

    @Autowired
    private MovieRepository repository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private MovieService service;

    @Autowired
    public CustomRepositoryRestController(
        PagedResourcesAssembler<Object> pagedResourcesAssembler) {
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

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
    @PostMapping("/resources/transfer")
    public Set<Integer> transferResources(@RequestBody TransferParam transferParam) {
        return service.transferResources(transferParam.getResourceIds(), transferParam.getMovieId());
    }

    @GetMapping("/persons/search")
    public List<String> findPersons(String name) {
        return personRepository.findTop20ByNameContains(name).stream().map(Person::getName).collect(Collectors.toList());
    }

    @GetMapping("/movies")
    public Resources<?> getMovies(@RequestParam(value = "q", required = false) String search,
        DefaultedPageable pageable, PersistentEntityResourceAssembler assembler) {

        Link baseLink = entityLinks.linkToPagedResource(Movie.class,
            pageable.isDefault() ? null : pageable.getPageable());

        Iterable<Movie> results;
        if (StringUtils.isEmpty(search)) {
            results = repository.findAll(pageable.getPageable());
        } else {
            Node rootNode = new RSQLParser(RsqlSearchOperation.operators()).parse(search);
            Specification<Movie> spec = rootNode.accept(new CustomRsqlVisitor<>());
            results = repository.findAll(spec, pageable.getPageable());
        }

        return toResources(results, assembler, Movie.class, baseLink);
    }

    @SuppressWarnings({"unchecked"})
    private Resources<?> toResources(Iterable<?> source,
        PersistentEntityResourceAssembler assembler,
        Class<?> domainType, Link baseLink) {

        if (source instanceof Page) {
            Page<Object> page = (Page<Object>) source;
            return entitiesToResources(page, assembler, domainType, baseLink);
        } else {
            return new Resources(EMPTY_RESOURCE_LIST);
        }
    }

    private Resources<?> entitiesToResources(
        Page<Object> page, PersistentEntityResourceAssembler assembler,
        Class<?> domainType, Link baseLink) {

        if (page.getContent().isEmpty()) {
            return pagedResourcesAssembler.toEmptyResource(page, domainType, baseLink);
        }

        return baseLink == null ? pagedResourcesAssembler.toResource(page, assembler)
            : pagedResourcesAssembler.toResource(page, assembler, baseLink);
    }

}
