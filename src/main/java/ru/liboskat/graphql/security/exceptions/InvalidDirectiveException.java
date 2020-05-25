package ru.liboskat.graphql.security.exceptions;

public class InvalidDirectiveException extends RuntimeException {
    public InvalidDirectiveException() {
        super("Invalid auth directive. Directive may add automatically. Correct is " + System.lineSeparator() +
                "directive @auth(rw : String, r : String, w : String) on SCHEMA | OBJECT | FIELD_DEFINITION | ARGUMENT_DEFINITION | INPUT_OBJECT | INPUT_FIELD_DEFINITION ");
    }
}
