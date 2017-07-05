package org.har01d.imovie.web.user;

import javax.validation.constraints.Size;
import lombok.Data;
import org.har01d.imovie.web.validation.UniqueEmail;
import org.har01d.imovie.web.validation.UniqueUsername;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class AccountDTO {

    @NotBlank
    @UniqueUsername(message = "Username already exists")
    @Size(min = 5, max = 35, message = "Username have to be grater than 5 characters")
    private String username;

    @NotBlank
    @Size(min = 8, max = 255, message = "Password have to be grater than 8 characters")
    private String password;

    @NotBlank
    @Size(min = 6, max = 64)
    @Email
    @UniqueEmail(message = "Email already exists")
    private String email;

}
