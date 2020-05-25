package ru.liboskat.graphql.security.execution;

import ru.liboskat.graphql.security.storage.TokenExpression;

import java.util.Map;

public interface TokenExpressionSolver {
    boolean solve(TokenExpression expression, SecurityContext context, Map<String, String> arguments);
}
