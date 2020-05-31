package ru.liboskat.graphql.security.expression.transforming;

import ru.liboskat.graphql.security.storage.token.OperatorToken;
import ru.liboskat.graphql.security.storage.TokenExpression;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Реализация интерфейса {@link TokenExpressionCombiner}, используемая для объединения выражений, используя конъюнкцию
 */
public class TokenExpressionConjunctCombiner implements TokenExpressionCombiner {
    /**
     * @param tokenExpressions выражения в обратной польской записи
     * @return конъюнкция списка выражений
     */
    @Override
    public TokenExpression combine(List<TokenExpression> tokenExpressions) {
        TokenExpression result = new TokenExpression();
        //убираем все пустые выражения
        List<TokenExpression> notEmpty = tokenExpressions.stream()
                .filter(Objects::nonNull)
                .filter(tokenExpression -> !tokenExpression.isEmpty())
                .collect(Collectors.toList());
        //для первого элемента добавляем все токены из выражения
        notEmpty.stream().findFirst().ifPresent(tokenExpression -> result.addAllTokens(tokenExpression.getTokens()));
        //для последующих добавляем все токены из выражения и оператор AND
        notEmpty.stream().skip(1).forEachOrdered(tokenExpression -> {
            result.addAllTokens(tokenExpression.getTokens());
            result.addToken(OperatorToken.AND);
        });
        return result;
    }
}
