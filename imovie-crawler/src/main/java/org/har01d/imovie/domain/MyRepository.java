package org.har01d.imovie.domain;

import java.io.Serializable;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface MyRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {

    Optional<T> findFirstByName(String name);
}
