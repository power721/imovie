package org.har01d.imovie.web.user;

import javax.validation.Valid;
import org.har01d.imovie.web.user.domain.User;
import org.har01d.imovie.web.user.domain.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@RepositoryRestController
public class UserController {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final LocalValidatorFactoryBean validator;

    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.addValidators(validator);
    }

    @Autowired
    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder,
        LocalValidatorFactoryBean validator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @PostMapping("/users/signup")
    public @ResponseBody User signup(@Valid @RequestBody AccountDTO accountDTO) {
        User user = new User();
        user.setUsername(accountDTO.getUsername());
        user.setEmail(accountDTO.getEmail());
        user.setPassword(passwordEncoder.encode(accountDTO.getPassword()));

        return userRepository.save(user);
    }

}
