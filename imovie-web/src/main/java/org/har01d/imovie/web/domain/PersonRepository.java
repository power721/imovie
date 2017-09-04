package org.har01d.imovie.web.domain;

import java.util.List;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(excerptProjection = MyExcerpt.class)
public interface PersonRepository extends MyRepository<Person, Integer> {

    List<Person> findTop20ByNameContains(@Param("name") String name);
}
