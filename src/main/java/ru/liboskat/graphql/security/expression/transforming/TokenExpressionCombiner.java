package ru.liboskat.graphql.security.expression.transforming;

import ru.liboskat.graphql.security.storage.TokenExpression;

import java.util.Arrays;
import java.util.List;

/**
 * Interface that is used to write classes that combine {@link TokenExpression} objects
 */
public interface TokenExpressionCombiner {
    /**
     * Combines two {@link TokenExpression}
     * @return combined {@link TokenExpression}
     */
    default TokenExpression combine(TokenExpression firstExpression, TokenExpression secondExpression) {
        return combine(Arrays.asList(firstExpression, secondExpression));
    }

    /**
     * Combines list of {@link TokenExpression}
     * @return combined {@link TokenExpression}
     */
    TokenExpression combine(List<TokenExpression> tokenExpressions);
}
