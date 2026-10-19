package com.nest.exception;

/** The caller can see the resource but is not allowed to perform this operation on it. */
public class ForbiddenOperationException extends NestException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
