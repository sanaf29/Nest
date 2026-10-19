package com.nest.exception;

/** Login failed. The message is deliberately the same for an unknown email and a wrong password. */
public class InvalidCredentialsException extends NestException {

    public InvalidCredentialsException() {
        super("Invalid email or password.");
    }
}
