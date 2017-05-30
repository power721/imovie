package org.har01d.imovie.domain;

import java.util.Date;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "list", types = {Movie.class})
public interface MovieExcerpt {
    Integer getId();

    String getTitle();

    String getSynopsis();

    String getThumb();

    String getDbUrl();

    String getDbScore();

    String getImdbUrl();

    String getImdbScore();

    Date getCreatedTime();
}
