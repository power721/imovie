package org.har01d.imovie.domain;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.validation.constraints.NotNull;

@Entity
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    private String name;

    @ManyToMany(mappedBy = "directors", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Movie> directMovies;

    @ManyToMany(mappedBy = "editors", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Movie> editMovies;

    @ManyToMany(mappedBy = "actors", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Movie> actMovies;

    public Person() {
    }

    public Person(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Movie> getDirectMovies() {
        return directMovies;
    }

    public List<Movie> getEditMovies() {
        return editMovies;
    }

    public List<Movie> getActMovies() {
        return actMovies;
    }
//    @JsonView(JsonViews.List.class)
//    public List<Movie> getMovies() {
//        List<Movie> movies = new ArrayList<>();
//        movies.addAll(directMovies);
//        movies.addAll(editMovies);
//        movies.addAll(actMovies);
//        return movies;
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Person person = (Person) o;

        return name.equals(person.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
