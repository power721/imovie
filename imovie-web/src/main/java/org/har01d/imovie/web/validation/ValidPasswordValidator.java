package org.har01d.imovie.web.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.har01d.imovie.web.user.UserDTO;
import org.har01d.imovie.web.user.domain.UserRepository;
import org.har01d.imovie.web.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, UserDTO> {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void initialize(ValidPassword constraintAnnotation) {

    }

    @Override
    public boolean isValid(UserDTO userDTO, ConstraintValidatorContext context) {
        if (userDTO == null || userDTO.getOldPassword() == null) {
            return false;
        }

        String username = SecurityUtils.currentUsername();
        String password = userRepository.findByUsername(username).getPassword();
        return passwordEncoder.matches(userDTO.getOldPassword(), password);
    }

}
