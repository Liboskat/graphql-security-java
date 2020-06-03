package ru.liboskat.graphql.security.storage.token;

/**
 * Хранит информацию об операторе
 */
public enum OperatorToken implements Token {
    LEFT_PARENTHESIS("("),
    RIGHT_PARENTHESIS(")"),
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
     * @return приоритет оператора
     */
    public int getPrecedence() {
        return precedence;
    }

    @Override
    public String toString() {
        return stringRepresentation;
    }
}
