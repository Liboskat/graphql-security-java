package ru.liboskat.graphql.security.expression.transforming;

import ru.liboskat.graphql.security.storage.TokenExpression;

/**
 * Интерфейс для реализации классов, конвертирующих выражения {@link TokenExpression} в обратную польскую запись
 */
public interface RpnExpressionConverter {
    /**
     * @param tokenExpression некое выражение
     * @return выражение в обратной польской записи
     */
    TokenExpression convertToRpn(TokenExpression tokenExpression);
}
