package ru.liboskat.graphql.security.expression.transforming;

import ru.liboskat.graphql.security.storage.OperatorToken;
import ru.liboskat.graphql.security.storage.TokenExpression;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TokenExpressionConjunctCombiner implements TokenExpressionCombiner {
    @Override
    public TokenExpression combine(List<TokenExpression> tokenExpressions) {
        TokenExpression result = new TokenExpression();
        List<TokenExpression> notEmpty = tokenExpressions.stream()
                .filter(Objects::nonNull)
                .filter(tokenExpression -> !tokenExpression.isEmpty())
                .collect(Collectors.toList());
        notEmpty.stream().findFirst().ifPresent(tokenExpression -> result.addAllTokens(tokenExpression.getTokens()));
        notEmpty.stream().skip(1).forEachOrdered(tokenExpression -> {
            result.addAllTokens(tokenExpression.getTokens());
            result.addToken(OperatorToken.AND);
        });
        return result;
    }
}
