package ru.liboskat.graphql.security.exceptions;

/**
 * Выбрасывается если директива @auth некорректа
 */
public class InvalidAuthDirectiveException extends RuntimeException {
    /**
     * Создает исключение с сообщением по умолчанию
     */
    public InvalidAuthDirectiveException() {
        super("Invalid auth directive. Directive may add automatically. Correct is " + System.lineSeparator() +
                "directive @auth(rw : String, r : String, w : String) on SCHEMA | OBJECT | FIELD_DEFINITION | ARGUMENT_DEFINITION | INPUT_OBJECT | INPUT_FIELD_DEFINITION ");
    }
}
