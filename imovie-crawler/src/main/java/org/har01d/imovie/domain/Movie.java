package org.har01d.imovie.domain;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    private Integer id;

    @NotNull
    private String title;

    @NotNull
    private String name;

    private Integer year;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Region> regions;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    private String thumb;

    private String cover;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Resource> resources = new HashSet<>();
    private int size;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Category> categories;

    @ElementCollection(fetch = FetchType.LAZY)
    private Set<String> aliases = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Person> directors;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Person> editors;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Person> actors;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Language> languages;

    @ElementCollection(fetch = FetchType.LAZY)
    @Column(columnDefinition = "TEXT")
    private Set<String> snapshots;

    private String releaseDate;

    private String runningTime;

    private Integer season;
    private Integer episode;

    private String imdbUrl;
    private String imdbScore;

    private String dbUrl;
    private String dbScore;
    private Integer db250;
    private Integer votes;

    private String website;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdTime = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedTime;

    private transient Date sourceTime = new Date();
    private transient int newResources;
    private transient boolean completed = true;

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

    public Set<Resource> getRes() {
        return resources;
    }

    public void setResources(Set<Resource> resources) {
        this.resources = resources;
    }

    public void addResources(Collection<Resource> resources) {
        int size = this.resources.size();
        this.resources.addAll(resources);
        this.newResources += this.resources.size() - size;
    }

    public void addResource(Resource resource) {
        if (this.resources.add(resource)) {
            this.newResources++;
        }
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

    public void addAliases(Set<String> aliases) {
        this.aliases.addAll(aliases);
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

    public Integer getSeason() {
        return season;
    }

    public void setSeason(Integer season) {
        this.season = season;
    }

    public Integer getEpisode() {
        return episode;
    }

    public void setEpisode(Integer episode) {
        this.episode = episode;
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

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public Integer getDb250() {
        return db250;
    }

    public void setDb250(Integer db250) {
        this.db250 = db250;
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

    public Date getSourceTime() {
        return sourceTime;
    }

    public void setSourceTime(Date sourceTime) {
        this.sourceTime = sourceTime;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getNewResources() {
        return newResources;
    }

    public void setNewResources(int newResources) {
        this.newResources = newResources;
    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
