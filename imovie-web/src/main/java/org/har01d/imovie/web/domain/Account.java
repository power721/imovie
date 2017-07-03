package org.har01d.imovie.web.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import org.har01d.imovie.web.validation.UniqueUsername;

@Data
@Entity
public class Account {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    @UniqueUsername(message = "Username already exists")
    @Size(min = 6, max = 255, message = "Username have to be grater than 6 characters")
    @Column(unique = true)
    private String username;

    @NotNull
    @Size(min = 8, max = 255, message = "Password have to be grater than 8 characters")
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

    public Account() {

    }

    public Account(String username) {
        this.username = username;
    }

}
