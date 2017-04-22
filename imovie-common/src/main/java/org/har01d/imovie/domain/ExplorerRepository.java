package org.har01d.imovie.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExplorerRepository extends JpaRepository<Explorer, Integer> {

    List<Explorer> findByType(String type);
}
