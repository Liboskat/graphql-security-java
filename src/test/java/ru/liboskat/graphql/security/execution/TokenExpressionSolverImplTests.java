package ru.liboskat.graphql.security.execution;

import org.junit.jupiter.api.Test;
import ru.liboskat.graphql.security.expression.parsing.ExpressionParser;
import ru.liboskat.graphql.security.expression.parsing.SimpleExpressionParser;
import ru.liboskat.graphql.security.expression.transforming.RpnExpressionConverter;
import ru.liboskat.graphql.security.expression.transforming.ShuntingYardExpressionConverter;
import ru.liboskat.graphql.security.storage.TokenExpression;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenExpressionSolverImplTests {
    private final TokenExpressionSolver tokenExpressionSolver = new TokenExpressionSolverImpl();

    private final ExpressionParser expressionParser = new SimpleExpressionParser();
    private final RpnExpressionConverter rpnExpressionConverter = new ShuntingYardExpressionConverter();

    @Test
    void solve_correctSimpleEqualExpression_withContextVariable_shouldReturnTrue() {
        String expression = "a = 'a'";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", "a")
                .build();
        assertTrue(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_correctSimpleEqualExpression_withContextVariableEnum_shouldReturnTrue() {
        String expression = "a = 'A'";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", TestEnum.A)
                .build();
        assertTrue(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_incorrectSimpleEqualExpression_withContextVariable_shouldReturnFalse() {
        String expression = "a = 'a'";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", "b")
                .build();
        assertFalse(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_correctSimpleNotEqualExpression_withContextVariable_shouldReturnTrue() {
        String expression = "a != 'a'";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", "b")
                .build();
        assertTrue(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_incorrectSimpleNotEqualExpression_withContextVariable_shouldReturnFalse() {
        String expression = "a != 'a'";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", "a")
                .build();
        assertFalse(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_correctSimpleEqualExpression_withGraphQLArgument_shouldReturnTrue() {
        String expression = "$a = 'a'";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        Map<String, String> args = new HashMap<>();
        args.put("a", "a");
        assertTrue(tokenExpressionSolver.solve(tokenExpression, SecurityContext.newSecurityContext().build(), args));
    }

    @Test
    void solve_incorrectSimpleEqualExpression_withGraphQLArgument_shouldReturnFalse() {
        String expression = "$a = 'a'";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        Map<String, String> args = new HashMap<>();
        args.put("a", "b");
        assertFalse(tokenExpressionSolver.solve(tokenExpression, SecurityContext.newSecurityContext().build(), args));
    }

    @Test
    void solve_correctSimpleEqualExpression_withNull_shouldReturnTrue() {
        String expression = "a = {null}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", null)
                .build();
        assertTrue(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_incorrectSimpleEqualExpression_withNull_shouldReturnFalse() {
        String expression = "a = {null}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", "b")
                .build();
        assertFalse(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_correctSimpleEqualExpression_withBoolean_shouldReturnTrue() {
        String expression = "a = {true}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", true)
                .build();
        assertTrue(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_incorrectSimpleEqualExpression_withBoolean_shouldReturnFalse() {
        String expression = "a = {true}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", false)
                .build();
        assertFalse(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_correctSimpleEqualExpression_withInteger_shouldReturnTrue() {
        String expression = "a = {5}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", 5)
                .build();
        assertTrue(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_incorrectSimpleEqualExpression_withInteger_shouldReturnFalse() {
        String expression = "a = {5}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", "a")
                .build();
        assertFalse(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_correctSimpleComparisonExpression_withInteger_shouldReturnTrue() {
        String expression = "a > {5}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", 6)
                .build();
        assertTrue(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_incorrectSimpleComparisonExpression_withInteger_shouldReturnFalse() {
        String expression = "a < {5}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", 6)
                .build();
        assertFalse(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_correctSimpleEqualExpression_withDouble_shouldReturnTrue() {
        String expression = "a = {5.9}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", 5.9)
                .build();
        assertTrue(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_incorrectSimpleEqualExpression_withDouble_shouldReturnFalse() {
        String expression = "a = {5.9}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", 5.3)
                .build();
        assertFalse(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_correctSimpleComparisonExpression_withDouble_shouldReturnTrue() {
        String expression = "a >= {5.9}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", 5.9)
                .build();
        assertTrue(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_incorrectSimpleComparisonExpression_withDouble_shouldReturnFalse() {
        String expression = "a <= {5.9}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", 6)
                .build();
        assertFalse(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_correctSimpleEqualExpression_withDate_shouldReturnTrue() {
        String expression = "a = {2020-12-16}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", LocalDate.parse("2020-12-16"))
                .build();
        assertTrue(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_incorrectSimpleEqualExpression_withDate_shouldReturnFalse() {
        String expression = "a = {2020-12-16}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", LocalDate.parse("2020-12-18"))
                .build();
        assertFalse(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_correctSimpleComparisonExpression_withDate_shouldReturnTrue() {
        String expression = "a < {2020-12-16}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", LocalDateTime.parse("2018-12-16T16:30"))
                .build();
        assertTrue(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_incorrectSimpleComparisonExpression_withDate_shouldReturnFalse() {
        String expression = "a > {2020-12-16}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("a", ZonedDateTime.parse("2018-12-16T16:30+02:00"))
                .build();
        assertFalse(tokenExpressionSolver.solve(tokenExpression, securityContext, new HashMap<>()));
    }

    @Test
    void solve_correctComplexExpression_shouldReturnTrue() {
        String expression = "(date < {2020-12-16} & false != {true} & (integer < {2} | string NOT IN ('string', 'string1'))) " +
                "| null = {null} | double > {1}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("date", ZonedDateTime.parse("2018-12-16T16:30+02:00"))
                .field("integer", 3)
                .field("double", 3.2)
                .field("null", null)
                .field("false", false)
                .build();
        Map<String, String> arguments = new HashMap<>();
        arguments.put("string", "string");
        assertTrue(tokenExpressionSolver.solve(tokenExpression, securityContext, arguments));
    }

    @Test
    void solve_incorrectComplexExpression_shouldReturnFalse() {
        String expression = "(date < {2020-12-16} & false != {true} &(integer < {2} | string NOT IN ('string', 'string1'))) " +
                "& null = {null} & double > {1}";
        TokenExpression tokenExpression = rpnExpressionConverter.convertToRpn(expressionParser.parse(expression));
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("date", ZonedDateTime.parse("2018-12-16T16:30+02:00"))
                .field("integer", 1)
                .field("double", 0.2)
                .field("null", null)
                .field("false", false)
                .build();
        Map<String, String> arguments = new HashMap<>();
        arguments.put("string", "string");
        assertFalse(tokenExpressionSolver.solve(tokenExpression, securityContext, arguments));
    }

    private enum TestEnum {
        A
    }
}
