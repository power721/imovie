package org.har01d.imovie.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExplorerRepository extends JpaRepository<Explorer, String> {
    Page<Explorer> findByType(String type, Pageable pageable);
}
