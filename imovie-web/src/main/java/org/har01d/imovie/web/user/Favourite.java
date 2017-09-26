package org.har01d.imovie.web.user;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import org.har01d.imovie.web.domain.Movie;

public class Favourite {

    private Movie movie;

    public Favourite(Movie movie) {
        this.movie = movie;
    }

    public Integer getId() {
        return movie.getId();
    }

    public String getTitle() {
        return movie.getTitle();
    }

    public String getSynopsis() {
        return movie.getSynopsis();
    }

    public String getThumb() {
        return movie.getThumb();
    }

    public Set<CategoryDTO> getCategories() {
        return movie.getCategories().stream().map(CategoryDTO::new).collect(Collectors.toSet());
    }

    public int getSize() {
        return movie.getSize();
    }

    public String getDbUrl() {
        return movie.getDbUrl();
    }

    public String getDbScore() {
        return movie.getDbScore();
    }

    public String getImdbUrl() {
        return movie.getImdbUrl();
    }

    public String getImdbScore() {
        return movie.getImdbScore();
    }

    public Integer getEpisode() {
        return movie.getEpisode();
    }

    public Date getCreatedTime() {
        return movie.getCreatedTime();
    }

    public Date getUpdatedTime() {
        return movie.getUpdatedTime();
    }
}
