package org.har01d.imovie.web.service;

import java.util.Set;

public interface MovieService {
    void deleteResource(Integer id);

    Set<Integer> transferResources(Set<Integer> resourceIds, Integer movieId);

    void deleteMovie(Integer id);
}
