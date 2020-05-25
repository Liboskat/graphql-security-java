package ru.liboskat.graphql.security.exceptions;

public class InvalidExpressionException extends RuntimeException {
    private static final String MESSAGE_FORMAT = "%s in \"%s\"";
    private static final String POSITION_MESSAGE_FORMAT = "%s, position %d in \"%s\"";
    private static final String ILLEGAL_SYMBOL_FORMAT = "Illegal symbol '%c' on position %d, expected %s in \"%s\"";

    public InvalidExpressionException() {
        super();
    }

    public InvalidExpressionException(String message) {
        super(message);
    }

    public InvalidExpressionException(String message, String expression) {
        this(String.format(MESSAGE_FORMAT, message, expression));
    }

    public InvalidExpressionException(String message, int position, String expression) {
        this(String.format(POSITION_MESSAGE_FORMAT, message, position, expression));
    }

    public InvalidExpressionException(char symbol, int position, String expected, String expression) {
        this(String.format(ILLEGAL_SYMBOL_FORMAT, symbol, position, expected, expression));
    }
}
