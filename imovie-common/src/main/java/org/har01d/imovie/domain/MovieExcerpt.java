package org.har01d.imovie.domain;

import org.springframework.data.rest.core.config.Projection;

@Projection(name = "list", types = {Movie.class})
public interface MovieExcerpt {

    Integer getId();

    String getTitle();

    String getThumb();

    String getDbUrl();

    String getDbScore();
}
