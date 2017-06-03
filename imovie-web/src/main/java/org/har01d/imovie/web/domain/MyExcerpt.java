package org.har01d.imovie.web.domain;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "list", types = {Category.class, Language.class, Person.class, Region.class})
public interface MyExcerpt {
    Integer getId();

    String getName();
}
