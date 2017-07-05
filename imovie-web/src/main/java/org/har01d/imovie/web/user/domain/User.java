package org.har01d.imovie.web.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.har01d.imovie.web.domain.Role;
import org.hibernate.validator.constraints.Email;

@Data
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(unique = true)
    private String username;

    @NotNull
    @Email
    private String email;

    @NotNull
    @JsonIgnore
    private String password;

    @NotNull
    private boolean enabled = true;

    @NotNull
    private boolean credentialsExpired = false;

    @NotNull
    private boolean expired = false;

    @NotNull
    private boolean locked = false;

    @Enumerated(EnumType.STRING)
    private Role role = Role.ROLE_USER;

    public User() {

    }

    public User(String username) {
        this.username = username;
    }

}
