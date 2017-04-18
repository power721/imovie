package org.har01d.imovie.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Integer> {

    Event findFirstBySource(String source);
}
