package org.har01d.imovie.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

@Entity
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String uri;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String original;

    private String title;

    public Resource() {
    }

    public Resource(String uri) {
        this.uri = uri;
    }

    public Resource(String uri, String title) {
        this.uri = uri;
        this.title = title;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginal() {
        return original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Resource resource = (Resource) o;

        return uri.equals(resource.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }
}
