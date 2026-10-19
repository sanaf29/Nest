package com.nest.exception;

/**
 * The requested resource does not exist <em>or</em> is not owned by the caller.
 * Both cases are reported identically so resource IDs cannot be probed across users.
 */
public class ResourceNotFoundException extends NestException {

    public ResourceNotFoundException(String resourceName, Object id) {
        super("%s %s was not found.".formatted(resourceName, id));
    }
}
