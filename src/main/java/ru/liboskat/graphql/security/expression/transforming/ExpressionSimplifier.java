package ru.liboskat.graphql.security.expression.transforming;

import ru.liboskat.graphql.security.storage.TokenExpression;

/**
 * Interface that is used to write classes that simplify {@link TokenExpression}
 */
public interface ExpressionSimplifier {
    /**
     * @param tokenExpression some {@link TokenExpression}
     * @return simplified {@link TokenExpression}
     */
    TokenExpression simplify(TokenExpression tokenExpression);
}
