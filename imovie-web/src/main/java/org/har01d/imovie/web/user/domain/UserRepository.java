package org.har01d.imovie.web.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Integer> {

    User findByUsername(@Param("name") String username);

    User findByEmail(@Param("email") String email);
}
