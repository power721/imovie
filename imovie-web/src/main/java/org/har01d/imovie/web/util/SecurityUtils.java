package org.har01d.imovie.web.util;

import java.security.Principal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public final class SecurityUtils {

    public static String currentUsername() {
        Object principal = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof Principal) {
            username = ((Principal) principal).getName();
        } else {
            username = principal.toString();
        }
        return username;
    }

}
