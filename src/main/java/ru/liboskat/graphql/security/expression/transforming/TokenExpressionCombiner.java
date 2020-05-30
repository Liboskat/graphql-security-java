package ru.liboskat.graphql.security.expression.transforming;

import ru.liboskat.graphql.security.storage.TokenExpression;

import java.util.Arrays;
import java.util.List;

/**
 * Интерфейс, используемый для объединения выражений {@link TokenExpression}
 */
public interface TokenExpressionCombiner {
    /**
     * @return объединенное выражение {@link TokenExpression}
     */
    default TokenExpression combine(TokenExpression firstExpression, TokenExpression secondExpression) {
        return combine(Arrays.asList(firstExpression, secondExpression));
    }

    /**
     * Объединяет список выражений в одно
     * @return объединенное выражений {@link TokenExpression}
     */
    TokenExpression combine(List<TokenExpression> tokenExpressions);
}
