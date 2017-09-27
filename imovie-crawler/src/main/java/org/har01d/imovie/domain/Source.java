package org.har01d.imovie.domain;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

@Entity
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer movieId;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String uri;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdTime = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedTime;

    @Column
    private boolean completed;

    public Source() {
    }

    public Source(String uri) {
        this.uri = uri;
    }

    public Source(String uri, int id) {
        this.uri = uri;
        this.movieId = id;
    }

    public Source(String uri, boolean completed) {
        this.uri = uri;
        this.completed = completed;
    }

    public Source(String uri, Date createdTime) {
        this.uri = uri;
        this.createdTime = createdTime;
    }

    public Source(String uri, Date createdTime, boolean completed) {
        this.uri = uri;
        this.createdTime = createdTime;
        this.completed = completed;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Date updatedTime) {
        this.updatedTime = updatedTime;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
