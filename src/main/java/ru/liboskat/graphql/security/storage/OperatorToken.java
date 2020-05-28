package ru.liboskat.graphql.security.storage;

/**
 * An implementation of {@link Token} that is used to store information about operator
 */
public enum OperatorToken implements Token {
    LEFT_PAREN("("),
    RIGHT_PAREN(")"),
    AND(2, "&"),
    OR(1, "|"),
    NOT(3, "!");

    private int precedence;
    private final String stringRepresentation;

    OperatorToken(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    OperatorToken(int precedence, String stringRepresentation) {
        this.precedence = precedence;
        this.stringRepresentation = stringRepresentation;
    }

    /**
     * @return precedence of operator
     */
    public int getPrecedence() {
        return precedence;
    }

    @Override
    public String toString() {
        return stringRepresentation;
    }
}
