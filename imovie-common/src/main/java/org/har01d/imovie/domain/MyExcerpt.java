package org.har01d.imovie.domain;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "list", types = {Category.class, Language.class, Person.class, Region.class, Tag.class})
public interface MyExcerpt {

    Integer getId();

    String getName();
}
