package com.nest.security;

/**
 * Identifies the authenticated caller.
 *
 * <p>Controllers obtain the user id here and never from request parameters or bodies, so a
 * client cannot claim to be another user. Services receive the id explicitly, which keeps them
 * independent of Spring Security and easy to unit-test.
 */
public interface CurrentUserProvider {

    long currentUserId();
}
