package com.nest.exception;

/**
 * A well-formed request that violates a domain rule (for example, a negative opening balance
 * on a savings account). Thrown by domain objects and services.
 */
public class BusinessRuleException extends NestException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
