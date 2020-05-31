package ru.liboskat.graphql.security.execution;

import ru.liboskat.graphql.security.storage.TokenExpression;

import java.util.Map;

/**
 * Интерфейс для создания классов, решающих выражения {@link TokenExpression}
 */
public interface TokenExpressionSolver {
    /**
     * @param expression выражение для решения
     * @param context контекст безопасности
     * @param arguments аргументы запрашиваемого поля
     * @return результат решения выражения
     */
    boolean solve(TokenExpression expression, SecurityContext context, Map<String, String> arguments);
}
