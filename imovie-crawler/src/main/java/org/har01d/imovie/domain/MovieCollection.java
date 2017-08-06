package org.har01d.imovie.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Data
public class MovieCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    private String title;

    @NotNull
    private String author;

    @NotNull
    private String url;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @OneToMany
    private List<MovieComment> movieComments = new ArrayList<>();

    private int followers;

    private int recommend;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdTime = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedTime;

}
