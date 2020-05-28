package ru.liboskat.graphql.security.execution;

import ru.liboskat.graphql.security.storage.TokenExpression;

import java.util.Map;

/**
 * Interface that is used to write classes that solve {@link TokenExpression}
 */
public interface TokenExpressionSolver {
    /**
     * @param expression expression need to be solved
     * @param context security context of query
     * @param arguments arguments of field
     * @return result of solving the expression
     */
    boolean solve(TokenExpression expression, SecurityContext context, Map<String, String> arguments);
}
