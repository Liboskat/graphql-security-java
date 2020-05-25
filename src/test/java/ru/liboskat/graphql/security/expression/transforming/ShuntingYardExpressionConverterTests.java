package ru.liboskat.graphql.security.expression.transforming;

import org.junit.jupiter.api.Test;
import ru.liboskat.graphql.security.expression.transforming.RpnExpressionConverter;
import ru.liboskat.graphql.security.expression.transforming.ShuntingYardExpressionConverter;
import ru.liboskat.graphql.security.storage.ComparisonToken;
import ru.liboskat.graphql.security.storage.ComparisonToken.ComparisonType;
import ru.liboskat.graphql.security.storage.OperatorToken;
import ru.liboskat.graphql.security.storage.TokenExpression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.liboskat.graphql.security.storage.ComparisonToken.ValueType;

class ShuntingYardExpressionConverterTests {
    private final RpnExpressionConverter converter = new ShuntingYardExpressionConverter();

    @Test
    void convertToRpn_correctAnd_shouldConvertCorrectly() {
        checkTwoArgumentOperator(OperatorToken.AND);
    }

    @Test
    void convertToRpn_correctOR_shouldConvertCorrectly() {
        checkTwoArgumentOperator(OperatorToken.OR);
    }

    @Test
    void convertToRpn_correctNOT_shouldConvertCorrectly() {
        TokenExpression infix = new TokenExpression();
        infix.addToken(OperatorToken.NOT);
        infix.addToken(getSimpleComparisonToken("a"));

        TokenExpression rpn = new TokenExpression();
        rpn.addToken(getSimpleComparisonToken("a"));
        rpn.addToken(OperatorToken.NOT);
        assertEquals(rpn, converter.convertToRpn(infix));
    }

    @Test
    void convertToRpn_correctComplexExpression_shouldConvertCorrectly() {
        TokenExpression infix = new TokenExpression();
        infix.addToken(getSimpleComparisonToken("e"));
        infix.addToken(OperatorToken.AND);
        infix.addToken(getSimpleComparisonToken("f"));
        infix.addToken(OperatorToken.OR);
        infix.addToken(OperatorToken.NOT);
        infix.addToken(OperatorToken.LEFT_PAREN);
        infix.addToken(getSimpleComparisonToken("a"));
        infix.addToken(OperatorToken.AND);
        infix.addToken(getSimpleComparisonToken("b"));
        infix.addToken(OperatorToken.OR);
        infix.addToken(getSimpleComparisonToken("c"));
        infix.addToken(OperatorToken.AND);
        infix.addToken(getSimpleComparisonToken("d"));
        infix.addToken(OperatorToken.RIGHT_PAREN);

        TokenExpression rpn = new TokenExpression();
        rpn.addToken(getSimpleComparisonToken("e"));
        rpn.addToken(getSimpleComparisonToken("f"));
        rpn.addToken(OperatorToken.AND);
        rpn.addToken(getSimpleComparisonToken("a"));
        rpn.addToken(getSimpleComparisonToken("b"));
        rpn.addToken(OperatorToken.AND);
        rpn.addToken(getSimpleComparisonToken("c"));
        rpn.addToken(getSimpleComparisonToken("d"));
        rpn.addToken(OperatorToken.AND);
        rpn.addToken(OperatorToken.OR);
        rpn.addToken(OperatorToken.NOT);
        rpn.addToken(OperatorToken.OR);
        assertEquals(rpn, converter.convertToRpn(infix));
    }

    @Test
    void convertToRpn_withRightParensNumberMoreThanLeft_shouldThrowException() {
        TokenExpression infix = new TokenExpression();
        infix.addToken(getSimpleComparisonToken("a"));
        infix.addToken(OperatorToken.AND);
        infix.addToken(getSimpleComparisonToken("b"));
        infix.addToken(OperatorToken.RIGHT_PAREN);
        assertThrows(IllegalArgumentException.class, () -> converter.convertToRpn(infix));
    }

    private void checkTwoArgumentOperator(OperatorToken operator) {
        TokenExpression infix = new TokenExpression();
        infix.addToken(getSimpleComparisonToken("a"));
        infix.addToken(operator);
        infix.addToken(getSimpleComparisonToken("b"));
        TokenExpression rpn = new TokenExpression();
        rpn.addToken(getSimpleComparisonToken("a"));
        rpn.addToken(getSimpleComparisonToken("b"));
        rpn.addToken(operator);
        assertEquals(rpn, converter.convertToRpn(infix));
    }

    private ComparisonToken getSimpleComparisonToken(String value) {
        return ComparisonToken.builder()
                .firstValue(value, ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(value, ValueType.STRING)
                .comparisonType(ComparisonType.EQUALS)
                .build();
    }
}
