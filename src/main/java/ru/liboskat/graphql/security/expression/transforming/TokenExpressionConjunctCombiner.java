package ru.liboskat.graphql.security.expression.transforming;

import ru.liboskat.graphql.security.storage.token.OperatorToken;
import ru.liboskat.graphql.security.storage.TokenExpression;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An implementation of {@link TokenExpressionCombiner} that is used to conjunct expressions in Reversed Polish Notation
 */
public class TokenExpressionConjunctCombiner implements TokenExpressionCombiner {
    /**
     * @param tokenExpressions some expressions in Reversed Polish Notation
     * @return conjunction of expressions
     */
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
