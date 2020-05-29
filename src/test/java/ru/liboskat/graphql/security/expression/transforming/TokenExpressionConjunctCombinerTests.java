package ru.liboskat.graphql.security.expression.transforming;

import org.junit.jupiter.api.Test;
import ru.liboskat.graphql.security.storage.token.ComparisonToken;
import ru.liboskat.graphql.security.storage.token.OperatorToken;
import ru.liboskat.graphql.security.storage.token.Token;
import ru.liboskat.graphql.security.storage.TokenExpression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenExpressionConjunctCombinerTests {
    private final TokenExpressionCombiner combiner = new TokenExpressionConjunctCombiner();

    @Test
    void combine_shouldReturnConjunctionOfExpressions() {
        TokenExpression first = new TokenExpression();
        first.addToken(getSimpleComparisonToken("a"));

        TokenExpression second = new TokenExpression();
        second.addToken(getSimpleComparisonToken("b"));

        TokenExpression third = new TokenExpression();
        third.addToken(getSimpleComparisonToken("c"));

        List<Token> tokens = new ArrayList<>();
        tokens.addAll(first.getTokens());
        tokens.addAll(second.getTokens());
        tokens.add(OperatorToken.AND);
        tokens.addAll(third.getTokens());
        tokens.add(OperatorToken.AND);
        TokenExpression expectedExpression = new TokenExpression();
        expectedExpression.addAllTokens(tokens);

        TokenExpression combined = combiner.combine(Arrays.asList(first, second, third));
        assertEquals(expectedExpression, combined);
    }

    private ComparisonToken getSimpleComparisonToken(String value) {
        return ComparisonToken.builder()
                .firstValue(value, ComparisonToken.ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(value, ComparisonToken.ValueType.STRING)
                .comparisonType(ComparisonToken.ComparisonType.EQUALS)
                .build();
    }
}
