package org.har01d.imovie.web.security;

import java.util.Collection;
import java.util.Collections;
import org.har01d.imovie.web.domain.Account;
import org.har01d.imovie.web.domain.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository userRepository;

    @Autowired
    public CustomUserDetailsService(AccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = userRepository.findByUsername(username);

        if (account == null) {
            throw new UsernameNotFoundException("User " + username + " not found.");
        }

        if (account.getRole() == null) {
            throw new UsernameNotFoundException("User not authorized.");
        }

        GrantedAuthority authority = new SimpleGrantedAuthority(account.getRole().name());
        Collection<GrantedAuthority> grantedAuthorities = Collections.singletonList(authority);

        return new User(account.getUsername(),
            account.getPassword(), account.isEnabled(),
            !account.isExpired(), !account.isCredentialsExpired(),
            !account.isLocked(), grantedAuthorities);
    }

}
