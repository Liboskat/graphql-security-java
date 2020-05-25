package ru.liboskat.graphql.security.expression.transforming;

import org.junit.jupiter.api.Test;
import ru.liboskat.graphql.security.execution.TokenExpressionSolver;
import ru.liboskat.graphql.security.execution.TokenExpressionSolverImpl;
import ru.liboskat.graphql.security.expression.transforming.ExpressionSimplifier;
import ru.liboskat.graphql.security.expression.transforming.QuineMcCluskeyExpressionSimplifier;
import ru.liboskat.graphql.security.storage.ComparisonToken;
import ru.liboskat.graphql.security.storage.ComparisonToken.ValueType;
import ru.liboskat.graphql.security.storage.OperatorToken;
import ru.liboskat.graphql.security.storage.TokenExpression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class QuineMcCluskeyExpressionSimplifierTests {
    private final ExpressionSimplifier expressionSimplifier = new QuineMcCluskeyExpressionSimplifier();
    private final TokenExpressionSolver expressionSolver = new TokenExpressionSolverImpl();

    @Test
    void simplify_shouldHaveEqualFunction() {
        TokenExpression notSimplified = new TokenExpression();
        addSimpleEquality(notSimplified, "a");
        addSimpleEquality(notSimplified, "b");
        notSimplified.addToken(OperatorToken.AND);
        addSimpleEquality(notSimplified, "a");
        addSimpleEquality(notSimplified, "b");
        notSimplified.addToken(OperatorToken.AND);
        notSimplified.addToken(OperatorToken.OR);
        addSimpleEquality(notSimplified, "c");
        addSimpleEquality(notSimplified, "d");
        notSimplified.addToken(OperatorToken.AND);
        addSimpleEquality(notSimplified, "e");
        addSimpleEquality(notSimplified, "f");
        notSimplified.addToken(OperatorToken.AND);
        notSimplified.addToken(OperatorToken.OR);
        notSimplified.addToken(OperatorToken.NOT);
        notSimplified.addToken(OperatorToken.OR);
        addSimpleEquality(notSimplified, "e");
        addSimpleEquality(notSimplified, "f");
        notSimplified.addToken(OperatorToken.AND);
        addSimpleEquality(notSimplified, "b");
        notSimplified.addToken(OperatorToken.OR);
        notSimplified.addToken(OperatorToken.OR);

        TokenExpression simplified = expressionSimplifier.simplify(notSimplified);

        List<Boolean> notSimplifiedResults = new ArrayList<>();
        List<Boolean> simplifiedResults = new ArrayList<>();

        generateArgumentMaps().forEach(argumentMap -> {
            notSimplifiedResults.add(expressionSolver.solve(notSimplified, null, argumentMap));
            simplifiedResults.add(expressionSolver.solve(simplified, null, argumentMap));
        });
        assertIterableEquals(notSimplifiedResults, simplifiedResults);
    }

    private List<Map<String, String>> generateArgumentMaps(String... values) {
        int valuesSize = values.length;
        int rows = (int) Math.pow(2, valuesSize);
        List<Map<String, String>> result = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            Map<String, String> arguments = new HashMap<>();
            for (int j = 0; j < valuesSize; j++) {
                String value = values[j];
                boolean correct = (1 << j & i) != 0;
                if (correct) {
                    arguments.put(value, value);
                } else {
                    arguments.put(value, value + "!");
                }
            }
            result.add(arguments);
        }
        return result;
    }

    private void addSimpleEquality(TokenExpression expression, String value) {
        expression.addToken(ComparisonToken.builder()
                .firstValue(value, ValueType.GRAPHQL_ARGUMENT_NAME)
                .secondValue(value, ValueType.STRING)
                .build());
    }
}
