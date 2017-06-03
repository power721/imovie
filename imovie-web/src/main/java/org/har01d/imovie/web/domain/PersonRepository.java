package org.har01d.imovie.web.domain;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(excerptProjection = MyExcerpt.class)
public interface PersonRepository extends MyRepository<Person, Integer> {

}
