package org.har01d.imovie.web.service;

import java.util.Set;
import javax.validation.Valid;
import org.har01d.imovie.web.domain.Resource;
import org.har01d.imovie.web.dto.ResourceDTO;

public interface MovieService {
    void deleteResource(Integer id);

    Resource addResource(Integer id, @Valid ResourceDTO resourceDTO);

    Set<Integer> transferResources(Set<Integer> resourceIds, Integer movieId);

    void deleteMovie(Integer id);
}
