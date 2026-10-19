package com.nest.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Reads the user id from the {@code sub} claim of the validated JWT. */
@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return Long.parseLong(jwtAuthentication.getToken().getSubject());
        }
        throw new AuthenticationCredentialsNotFoundException("No authenticated user.");
    }
}
