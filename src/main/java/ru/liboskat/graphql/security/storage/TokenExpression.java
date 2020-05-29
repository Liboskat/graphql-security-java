package ru.liboskat.graphql.security.storage;

import ru.liboskat.graphql.security.storage.token.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Class that is used to store {@link Token} in order of addition
 */
public class TokenExpression {
    private final List<Token> tokens;

    /**
     * Creates empty expression
     */
    public TokenExpression() {
        this.tokens = new ArrayList<>();
    }

    /**
     * @return list of all tokens preserving order
     */
    public List<Token> getTokens() {
        return tokens;
    }

    /**
     * Adds token to expression
     * @param token some token
     */
    public void addToken(Token token) {
        tokens.add(token);
    }

    /**
     * Adds all tokens from list to expression
     * @param tokens list of tokens
     */
    public void addAllTokens(List<Token> tokens) {
        this.tokens.addAll(tokens);
    }

    /**
     * @return true if expression is empty, else false
     */
    public boolean isEmpty() {
        return tokens.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenExpression that = (TokenExpression) o;
        return Objects.equals(tokens, that.tokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokens);
    }

    @Override
    public String toString() {
        return tokens.stream().map(Objects::toString).collect(Collectors.joining());
    }
}
