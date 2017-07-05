package org.har01d.imovie.web.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.har01d.imovie.web.user.UserDTO;
import org.har01d.imovie.web.user.domain.User;
import org.har01d.imovie.web.user.domain.UserRepository;
import org.har01d.imovie.web.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class ChangeEmailValidator implements ConstraintValidator<UniqueEmail, UserDTO> {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void initialize(UniqueEmail constraintAnnotation) {

    }

    @Override
    public boolean isValid(UserDTO value, ConstraintValidatorContext context) {
        User user = userRepository.findByEmail(value.getEmail());
        String username = SecurityUtils.currentUsername();
        return user == null || user.getUsername().equals(username);
    }
}
