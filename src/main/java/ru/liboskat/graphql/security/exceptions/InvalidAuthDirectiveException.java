package ru.liboskat.graphql.security.exceptions;

/**
 * Thrown if @auth directive definition from schema is invalid
 */
public class InvalidAuthDirectiveException extends RuntimeException {
    /**
     * Creates exception with default message
     */
    public InvalidAuthDirectiveException() {
        super("Invalid auth directive. Directive may add automatically. Correct is " + System.lineSeparator() +
                "directive @auth(rw : String, r : String, w : String) on SCHEMA | OBJECT | FIELD_DEFINITION | ARGUMENT_DEFINITION | INPUT_OBJECT | INPUT_FIELD_DEFINITION ");
    }
}
