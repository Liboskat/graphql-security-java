package ru.liboskat.graphql.security.exceptions;

/**
 * This exception is thrown if there is unexpected internal library error
 */
public class InternalErrorException extends RuntimeException {
    public InternalErrorException(String message) {
        super(message);
    }
}
