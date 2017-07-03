package org.har01d.imovie.web.domain;

import lombok.Data;

@Data
public class MovieDto {

    private String name;
    private Category category;
    private Integer year;
}
