package ru.liboskat.graphql.security.expression.transforming;

import ru.liboskat.graphql.security.storage.TokenExpression;

/**
 * Интерфейс для создания классов, упрощающих {@link TokenExpression}
 */
public interface ExpressionSimplifier {
    /**
     * @param tokenExpression некое выражение
     * @return упрощенное выражение
     */
    TokenExpression simplify(TokenExpression tokenExpression);
}
