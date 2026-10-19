package com.nest.exception;

/** The request conflicts with existing state, such as a duplicate unique value. */
public class ConflictException extends NestException {

    public ConflictException(String message) {
        super(message);
    }
}
