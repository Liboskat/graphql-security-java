package ru.liboskat.graphql.security.storage;

import ru.liboskat.graphql.security.storage.token.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TokenExpression {
    private final List<Token> tokens;

    public TokenExpression() {
        this.tokens = new ArrayList<>();
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public void addToken(Token token) {
        tokens.add(token);
    }

    public void addAllTokens(List<Token> tokens) {
        this.tokens.addAll(tokens);
    }

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
