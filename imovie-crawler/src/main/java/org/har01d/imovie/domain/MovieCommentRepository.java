package org.har01d.imovie.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieCommentRepository extends JpaRepository<MovieComment, Integer> {

}
