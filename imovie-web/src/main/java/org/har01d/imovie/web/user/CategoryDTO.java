package org.har01d.imovie.web.user;

import org.har01d.imovie.web.domain.Category;

public class CategoryDTO {
    private Integer id;
    private String name;

    public CategoryDTO(Category category) {
        this.id = category.getId();
        this.name = category.getName();
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
