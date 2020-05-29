package ru.liboskat.graphql.security.expression.parsing;

import org.junit.jupiter.api.Test;
import ru.liboskat.graphql.security.exceptions.InvalidExpressionException;
import ru.liboskat.graphql.security.storage.token.ComparisonToken;
import ru.liboskat.graphql.security.storage.token.ComparisonToken.ComparisonType;
import ru.liboskat.graphql.security.storage.token.ComparisonToken.NullValue;
import ru.liboskat.graphql.security.storage.token.ComparisonToken.ValueType;
import ru.liboskat.graphql.security.storage.token.OperatorToken;
import ru.liboskat.graphql.security.storage.TokenExpression;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimpleExpressionParserTests {
    private final ExpressionParser expressionParser = new SimpleExpressionParser();

    @Test
    void parse_correctEquality_withContextVariableAndString_shouldBeParsedCorrectly() {
        String expression = "a = 'a'";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue("a", ValueType.STRING)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctEquality_withGraphQLArgumentAndInteger_shouldBeParsedCorrectly() {
        String expression = "$a = {1}";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_ARGUMENT_NAME)
                .secondValue(1, ValueType.INTEGER)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctEquality_withContextVariableAndDouble_shouldBeParsedCorrectly() {
        String expression = "a = {1.2}";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(1.2, ValueType.REAL)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctEquality_withContextVariableAndNull_shouldBeParsedCorrectly() {
        String expression = "a = {null}";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(NullValue.INSTANCE, ValueType.NULL)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctEquality_withContextVariableAndTrue_shouldBeParsedCorrectly() {
        String expression = "a = {true}";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(true, ValueType.BOOLEAN)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctEquality_withContextVariableAndFalse_shouldBeParsedCorrectly() {
        String expression = "a = {false}";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(false, ValueType.BOOLEAN)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctEquality_withContextVariableAndDate_shouldBeParsedCorrectly() {
        String expression = "a = {2020-10-10}";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(LocalDate.parse("2020-10-10"), ValueType.LOCAL_DATE)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctEquality_withContextVariableAndTime_shouldBeParsedCorrectly() {
        String expression = "a = {16:30:30}";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(LocalTime.parse("16:30:30"), ValueType.LOCAL_TIME)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctEquality_withContextVariableAndDateTime_shouldBeParsedCorrectly() {
        String expression = "a = {2020-10-10T16:30:30}";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(LocalDateTime.parse("2020-10-10T16:30:30"), ValueType.LOCAL_DATE_TIME)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctEquality_withContextVariableAndZonedDateTime_shouldBeParsedCorrectly() {
        String expression = "a = {2020-10-10T16:30:30+02:00}";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(ZonedDateTime.parse("2020-10-10T16:30:30+02:00"), ValueType.ZONED_DATE_TIME)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctEquality_withFirstLong_shouldBeParsedCorrectly() {
        String expression = "{5000000000} = $a";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue(5000000000L, ValueType.INTEGER)
                .secondValue("a", ValueType.GRAPHQL_ARGUMENT_NAME)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctEquality_withFirstString_shouldBeParsedCorrectly() {
        String expression = "'a' = a";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.STRING)
                .secondValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctEquality_withEscapedString_shouldBeParsedCorrectly() {
        //a = 'a\'\\'
        String expression = "a = 'a\\'\\\\'";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue("a'\\", ValueType.STRING)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctInequality_shouldBeParsedCorrectly() {
        String expression = "a != 'a'";
        TokenExpression correct = new TokenExpression();
        correct.addToken(OperatorToken.NOT);
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue("a", ValueType.STRING)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctLT_shouldBeParsedCorrectly() {
        String expression = "a < {1}";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(1, ValueType.INTEGER)
                .comparisonType(ComparisonType.LT)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctLTE_shouldBeParsedCorrectly() {
        String expression = "a <= {1}";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(1, ValueType.INTEGER)
                .comparisonType(ComparisonType.LTE)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctGT_shouldBeParsedCorrectly() {
        String expression = "a > {1}";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(1, ValueType.INTEGER)
                .comparisonType(ComparisonType.GT)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctGTE_shouldBeParsedCorrectly() {
        String expression = "a >= {1}";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(1, ValueType.INTEGER)
                .comparisonType(ComparisonType.GTE)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctAND_shouldBeParsedCorrectly() {
        String expression = "a >= {1} & a = 'a'";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(1, ValueType.INTEGER)
                .comparisonType(ComparisonType.GTE)
                .build()
        );
        correct.addToken(OperatorToken.AND);
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue("a", ValueType.STRING)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctOR_shouldBeParsedCorrectly() {
        String expression = "a >= {1} | a = 'a'";
        TokenExpression correct = new TokenExpression();
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(1, ValueType.INTEGER)
                .comparisonType(ComparisonType.GTE)
                .build()
        );
        correct.addToken(OperatorToken.OR);
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue("a", ValueType.STRING)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctNOT_shouldBeParsedCorrectly() {
        String expression = "!(a >= {1})";
        TokenExpression correct = new TokenExpression();
        correct.addToken(OperatorToken.NOT);
        correct.addToken(OperatorToken.LEFT_PAREN);
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(1, ValueType.INTEGER)
                .comparisonType(ComparisonType.GTE)
                .build()
        );
        correct.addToken(OperatorToken.RIGHT_PAREN);
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctIN_shouldBeParsedCorrectly() {
        String expression = "a IN ('b', 'c')";
        TokenExpression correct = new TokenExpression();
        correct.addToken(OperatorToken.LEFT_PAREN);
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue("b", ValueType.STRING)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        correct.addToken(OperatorToken.OR);
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue("c", ValueType.STRING)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        correct.addToken(OperatorToken.RIGHT_PAREN);
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctNotIN_shouldBeParsedCorrectly() {
        String expression = "a NOT IN ('b', 'c')";
        TokenExpression correct = new TokenExpression();
        correct.addToken(OperatorToken.LEFT_PAREN);
        correct.addToken(OperatorToken.NOT);
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue("b", ValueType.STRING)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        correct.addToken(OperatorToken.AND);
        correct.addToken(OperatorToken.NOT);
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue("c", ValueType.STRING)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        correct.addToken(OperatorToken.RIGHT_PAREN);
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_correctComplexExpression_shouldBeParsedCorrectly() {
        String expression = "!(a <= {1} | a = 'b') & (a >= {1} | a = 'a')";
        TokenExpression correct = new TokenExpression();
        correct.addToken(OperatorToken.NOT);
        correct.addToken(OperatorToken.LEFT_PAREN);
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(1, ValueType.INTEGER)
                .comparisonType(ComparisonType.LTE)
                .build()
        );
        correct.addToken(OperatorToken.OR);
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue("b", ValueType.STRING)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        correct.addToken(OperatorToken.RIGHT_PAREN);
        correct.addToken(OperatorToken.AND);
        correct.addToken(OperatorToken.LEFT_PAREN);
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(1, ValueType.INTEGER)
                .comparisonType(ComparisonType.GTE)
                .build()
        );
        correct.addToken(OperatorToken.OR);
        correct.addToken(ComparisonToken.builder()
                .firstValue("a", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue("a", ValueType.STRING)
                .comparisonType(ComparisonType.EQUALS)
                .build()
        );
        correct.addToken(OperatorToken.RIGHT_PAREN);
        assertEquals(correct, expressionParser.parse(expression));
    }

    @Test
    void parse_notClosedEquality_shouldThrowException() {
        String expression = "a = ";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_notClosedAnd_shouldThrowException() {
        String expression = "a = b &";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_notClosedParens_shouldThrowException() {
        String expression = "(a = b";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_notOpenedParens_shouldThrowException() {
        String expression = "a = b)";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_notClosedString_shouldThrowException() {
        String expression = "a = 'b";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_notClosedObject_shouldThrowException() {
        String expression = "a = {3";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_notParsableObject_shouldThrowException() {
        String expression = "a = {something}";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_illegalNOT_shouldThrowException() {
        String expression = "!a = {null}";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_notClosedIN_shouldThrowException() {
        String expression = "a IN ('b', 'c'";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_emptyGraphQLArgument_shouldThrowException() {
        String expression = "$ IN ('b', 'c')";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_illegalSymbol_stateWaitingFirstOperandOrOpeningBracketOrNegation_ShouldThrowException() {
        String expression = "?";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_illegalSymbol_stateWaitingComparisonOperator_ShouldThrowException() {
        String expression = "a ? ('b', 'c')";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_illegalSymbol_stateWaitingInValues_ShouldThrowException() {
        String expression = "a IN ?";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_illegalSymbol_stateWaitingCombiningOperatorOrClosingBracket_ShouldThrowException() {
        String expression = "a = b ?";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_emptyObject_ShouldThrowException() {
        String expression = "a = {}";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_illegalSymbol_stateReadingIN_ShouldThrowException() {
        String expression = "a I?";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_stringClosed_stateReadingIN_ShouldThrowException() {
        String expression = "a I";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_illegalSymbol_stateWaitingInValue_ShouldThrowException() {
        String expression = "a IN (?)";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }

    @Test
    void parse_illegalSymbol_stateWaitingInCommaOrBracket_ShouldThrowException() {
        String expression = "a IN ('a'?)";
        assertThrows(InvalidExpressionException.class, () -> expressionParser.parse(expression));
    }
}
