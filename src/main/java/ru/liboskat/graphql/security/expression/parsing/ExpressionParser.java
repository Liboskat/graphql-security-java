package ru.liboskat.graphql.security.expression.parsing;

import ru.liboskat.graphql.security.storage.TokenExpression;

/**
 * Интерфейс для создания классов, производящих парсинг строковых выражений
 */
public interface ExpressionParser {
    /**
     * @param expression некое строковое выражение
     * @return выражение в объектном виде
     */
    TokenExpression parse(String expression);
}
