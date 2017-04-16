package org.har01d.imovie.domain;

import java.util.Optional;

public interface CategoryRepository extends MyRepository<Category, Integer> {

    Optional<Category> findFirstByName(String name);
}
