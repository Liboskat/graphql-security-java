package ru.liboskat.graphql.security.expression.transforming;

import ru.liboskat.graphql.security.storage.TokenExpression;

import java.util.Arrays;
import java.util.List;

public interface TokenExpressionCombiner {
    default TokenExpression combine(TokenExpression firstExpression, TokenExpression secondExpression) {
        return combine(Arrays.asList(firstExpression, secondExpression));
    }

    TokenExpression combine(List<TokenExpression> tokenExpressions);
}
