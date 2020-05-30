package ru.liboskat.graphql.security.exceptions;

/**
 * Выбрасывается в случае внутренних неожиданных ошибок библиотеки
 */
public class InternalErrorException extends RuntimeException {
    public InternalErrorException() {
        super();
    }

    public InternalErrorException(String message) {
        super(message);
    }
}
