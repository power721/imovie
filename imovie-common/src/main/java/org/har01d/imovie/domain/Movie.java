package org.har01d.imovie.domain;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

@Entity
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(JsonViews.List.class)
    private Integer id;

    @NotNull
    @JsonView(JsonViews.List.class)
    private String title;

    @NotNull
    @JsonView(JsonViews.List.class)
    private String name;

    @JsonView(JsonViews.List.class)
    private Integer year;

    @ManyToMany(fetch = FetchType.EAGER)
    @JsonView(JsonViews.List.class)
    private Set<Region> regions;

    @Column(columnDefinition = "TEXT")
    @JsonView(JsonViews.List.class)
    private String synopsis;

    @JsonView(JsonViews.List.class)
    private String thumb;

    @JsonView(JsonViews.List.class)
    private String cover;

    @ManyToMany(fetch = FetchType.EAGER)
    @JsonView(JsonViews.Detail.class)
    private Set<Resource> resources = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JsonView(JsonViews.Detail.class)
    private Set<Category> categories;

    @ElementCollection(fetch = FetchType.EAGER)
    @JsonView(JsonViews.Detail.class)
    private Set<String> aliases = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JsonView(JsonViews.Detail.class)
    private Set<Tag> tags;

    @ManyToMany(fetch = FetchType.EAGER)
    @JsonView(JsonViews.Detail.class)
    private Set<Person> directors;

    @ManyToMany(fetch = FetchType.EAGER)
    @JsonView(JsonViews.Detail.class)
    private Set<Person> editors;

    @ManyToMany(fetch = FetchType.EAGER)
    @JsonView(JsonViews.Detail.class)
    private Set<Person> actors;

    @ManyToMany(fetch = FetchType.EAGER)
    @JsonView(JsonViews.Detail.class)
    private Set<Language> languages;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(columnDefinition = "TEXT")
    @JsonView(JsonViews.Detail.class)
    private Set<String> snapshots;

    @JsonView(JsonViews.Detail.class)
    private String releaseDate;

    @JsonView(JsonViews.Detail.class)
    private String runningTime;

    @JsonView(JsonViews.List.class)
    private String imdbUrl;
    @JsonView(JsonViews.List.class)
    private String imdbScore;

    @JsonView(JsonViews.List.class)
    private String dbUrl;
    @JsonView(JsonViews.List.class)
    private String dbScore;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonView(JsonViews.Detail.class)
    private Date createdTime = new Date();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Set<Region> getRegions() {
        return regions;
    }

    public void setRegions(Set<Region> regions) {
        this.regions = regions;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public void setResources(Set<Resource> resources) {
        this.resources = resources;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public void setCategories(Set<Category> categories) {
        this.categories = categories;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = aliases;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public Set<Person> getDirectors() {
        return directors;
    }

    public void setDirectors(Set<Person> directors) {
        this.directors = directors;
    }

    public Set<Person> getEditors() {
        return editors;
    }

    public void setEditors(Set<Person> editors) {
        this.editors = editors;
    }

    public Set<Person> getActors() {
        return actors;
    }

    public void setActors(Set<Person> actors) {
        this.actors = actors;
    }

    public void addActors(Set<Person> actors) {
        if (this.actors == null) {
            this.actors = new HashSet<>();
        }
        this.actors.addAll(actors);
    }

    public Set<Language> getLanguages() {
        return languages;
    }

    public void setLanguages(Set<Language> languages) {
        this.languages = languages;
    }

    public Set<String> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(Set<String> snapshots) {
        this.snapshots = snapshots;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getRunningTime() {
        return runningTime;
    }

    public void setRunningTime(String runningTime) {
        this.runningTime = runningTime;
    }

    public String getImdbUrl() {
        return imdbUrl;
    }

    public void setImdbUrl(String imdbUrl) {
        this.imdbUrl = imdbUrl;
    }

    public String getImdbScore() {
        return imdbScore;
    }

    public void setImdbScore(String imdbScore) {
        this.imdbScore = imdbScore;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getDbScore() {
        return dbScore;
    }

    public void setDbScore(String dbScore) {
        this.dbScore = dbScore;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }
}
