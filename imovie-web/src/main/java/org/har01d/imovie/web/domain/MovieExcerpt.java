package org.har01d.imovie.web.domain;

import java.util.Date;
import java.util.Set;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "list", types = {Movie.class})
public interface MovieExcerpt {

    Integer getId();

    String getTitle();

    String getSynopsis();

    String getThumb();

    Set<Category> getCategories();

    int getSize();

    String getDbUrl();

    String getDbScore();

    String getImdbUrl();

    String getImdbScore();

    Integer getEpisode();

    Date getCreatedTime();

    Date getUpdatedTime();
}
