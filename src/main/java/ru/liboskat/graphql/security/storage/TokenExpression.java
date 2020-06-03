package ru.liboskat.graphql.security.storage;

import ru.liboskat.graphql.security.storage.token.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Класс, для хранения выражения контроля доступа в объектном виде
 */
public class TokenExpression {
    private final List<Token> tokens;

    /**
     * Создает новое пустое выражение
     */
    public TokenExpression() {
        this.tokens = new ArrayList<>();
    }

    /**
     * Возвращает все токены выражения
     *
     * @return все токены выражения
     */
    public List<Token> getTokens() {
        return tokens;
    }

    /**
     * Добавляет {@link Token} в выражение
     *
     * @param token {@link Token} для добавления
     */
    public void addToken(Token token) {
        tokens.add(token);
    }

    /**
     * Добавляет все {@link Token} из списка в выражение
     *
     * @param tokens список {@link Token} для добавления
     */
    public void addAllTokens(List<Token> tokens) {
        this.tokens.addAll(tokens);
    }

    /**
     * Проверяет, является ли выражение пустым
     *
     * @return true, если нет токенов в выражении, иначе false
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
