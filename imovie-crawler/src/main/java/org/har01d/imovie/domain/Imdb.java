package org.har01d.imovie.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Imdb {

    @Id
    private String id;

    private String rating;

    public Imdb() {
    }

    public Imdb(String id, String rating) {
        this.id = id;
        this.rating = rating;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }
}
