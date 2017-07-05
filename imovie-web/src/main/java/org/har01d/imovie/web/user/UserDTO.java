package org.har01d.imovie.web.user;

import javax.validation.constraints.Size;
import lombok.Data;
import org.har01d.imovie.web.validation.UniqueEmail;
import org.har01d.imovie.web.validation.ValidPassword;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;


/**
 * for user update
 */
@UniqueEmail(message = "Email already exists")
@ValidPassword
@Data
public class UserDTO {

    @NotBlank
    private String oldPassword;

    @Size(min = 8, max = 255)
    private String password;

    @NotBlank
    @Size(min = 6, max = 64)
    @Email
    private String email;

}
