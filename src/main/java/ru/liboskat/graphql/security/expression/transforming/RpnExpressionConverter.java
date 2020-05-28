package ru.liboskat.graphql.security.expression.transforming;

import ru.liboskat.graphql.security.storage.TokenExpression;

/**
 * Interface that is used to convert {@link TokenExpression} to Reversed Polish Notation
 */
public interface RpnExpressionConverter {
    /**
     * @param tokenExpression some {@link TokenExpression}
     * @return {@link TokenExpression} in Reversed Polish Notation
     */
    TokenExpression convertToRpn(TokenExpression tokenExpression);
}
