package com.nest.security;

import com.nest.model.User;

/**
 * Issues access tokens for authenticated users.
 * Callers do not know the token format (JWT today), which keeps {@code AuthService} independent of Nimbus.
 */
public interface TokenService {

    AccessToken issue(User user);
}
