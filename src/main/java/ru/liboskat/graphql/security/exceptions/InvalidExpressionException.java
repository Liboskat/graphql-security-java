package ru.liboskat.graphql.security.exceptions;

/**
 * Thrown if access control expression is invalid
 */
public class InvalidExpressionException extends RuntimeException {
    private static final String MESSAGE_FORMAT = "%s in \"%s\"";
    private static final String POSITION_MESSAGE_FORMAT = "%s, position %d in \"%s\"";
    private static final String ILLEGAL_SYMBOL_FORMAT = "Illegal symbol '%c' on position %d, expected %s in \"%s\"";

    /**
     * Creates exception with expression and error message
     * @param message error message
     * @param expression parsed expression
     */
    public InvalidExpressionException(String message, String expression) {
        super(String.format(MESSAGE_FORMAT, message, expression));
    }

    /**
     * Creates exception with expression, error message and position of mistake
     * @param position position of mistake in expression
     * @param message error message
     * @param expression parsed expression
     */
    public InvalidExpressionException(String message, int position, String expression) {
        super(String.format(POSITION_MESSAGE_FORMAT, message, position, expression));
    }

    /**
     * Creates exception with expression, unexpected symbol, position of mistake, message with
     * information about expected token
     * @param symbol unexpected symbol
     * @param position position of mistake
     * @param expected message with information about expected token
     * @param expression parsed expression
     */
    public InvalidExpressionException(char symbol, int position, String expected, String expression) {
        super(String.format(ILLEGAL_SYMBOL_FORMAT, symbol, position, expected, expression));
    }
}
