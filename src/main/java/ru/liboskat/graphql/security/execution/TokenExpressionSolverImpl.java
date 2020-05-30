package ru.liboskat.graphql.security.execution;

import ru.liboskat.graphql.security.storage.TokenExpression;
import ru.liboskat.graphql.security.storage.token.ComparisonToken;
import ru.liboskat.graphql.security.storage.token.ComparisonToken.ComparisonType;
import ru.liboskat.graphql.security.storage.token.OperatorToken;
import ru.liboskat.graphql.security.storage.token.Token;
import ru.liboskat.graphql.security.utils.TemporalToZonedDateTimeConverter;

import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Реализация {@link TokenExpressionSolver}, используемая для решения выражений {@link TokenExpression}
 * в обратной польской записи
 */
public class TokenExpressionSolverImpl implements TokenExpressionSolver {
    /**
     * Решает выражение в обратной польской записи
     *
     * @param expression выражение в обратное польской записи
     * @param context    контекст безопасности запроса
     * @param arguments  аргументы поля, могут быть null
     * @return результат решения выражения
     * @throws IllegalArgumentException, если выражение неверное
     */
    @Override
    public boolean solve(TokenExpression expression, SecurityContext context, Map<String, String> arguments) {
        LinkedList<Boolean> stack = new LinkedList<>();
        for (Token token : expression.getTokens()) {
            if (token instanceof ComparisonToken) {
                stack.push(getResult((ComparisonToken) token, context, arguments));
            }
            if (token instanceof OperatorToken) {
                OperatorToken operation = ((OperatorToken) token);
                if (OperatorToken.OR == operation) {
                    boolean first = stack.pop();
                    boolean second = stack.pop();
                    stack.push(first || second);
                }
                if (OperatorToken.AND == operation) {
                    boolean first = stack.pop();
                    boolean second = stack.pop();
                    stack.push(first && second);
                }
                if (OperatorToken.NOT == operation) {
                    boolean first = stack.pop();
                    stack.push(!first);
                }
            }
        }
        boolean result;
        try {
            result = stack.pop();
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("Illegal TokenExpression");
        }
        return result;
    }

    private boolean getResult(ComparisonToken token, SecurityContext ctx, Map<String, String> arguments) {
        Object firstValue = getValue(token.getFirstValue(), token.getFirstValueType(), ctx, arguments);
        Object secondValue = getValue(token.getSecondValue(), token.getSecondValueType(), ctx, arguments);
        if (token.getComparisonType() == ComparisonType.EQUALS) {
            return isEquals(firstValue, secondValue);
        } else {
            return compare(firstValue, secondValue, token.getComparisonType());
        }
    }

    private boolean isEquals(Object firstValue, Object secondValue) {
        if (firstValue instanceof Number && secondValue instanceof Number) {
            Number firstNumber = (Number) firstValue;
            Number secondNumber = (Number) secondValue;
            return firstNumber.doubleValue() == secondNumber.doubleValue();
        } else if (firstValue instanceof Temporal && secondValue instanceof Temporal) {
            ZonedDateTime firstZonedDateTime = TemporalToZonedDateTimeConverter.convert((Temporal) firstValue);
            ZonedDateTime secondZonedDateTime = TemporalToZonedDateTimeConverter.convert((Temporal) secondValue);
            return firstZonedDateTime.equals(secondZonedDateTime);
        } else {
            return Objects.equals(firstValue, secondValue);
        }
    }

    private boolean compare(Object firstValue, Object secondValue, ComparisonType comparisonType) {
        int compareResult;
        if (firstValue instanceof Temporal && secondValue instanceof Temporal) {
            ZonedDateTime firstZonedDateTime = TemporalToZonedDateTimeConverter.convert((Temporal) firstValue);
            ZonedDateTime secondZonedDateTime = TemporalToZonedDateTimeConverter.convert((Temporal) secondValue);
            compareResult = firstZonedDateTime.compareTo(secondZonedDateTime);
        } else if (firstValue instanceof Number && secondValue instanceof Number) {
            Number firstNumber = (Number) firstValue;
            Number secondNumber = (Number) secondValue;
            compareResult = Double.compare(firstNumber.doubleValue(), secondNumber.doubleValue());
        } else {
            throw new IllegalArgumentException(String.format("Can't compare %s and %s with operation %s",
                    firstValue, secondValue, comparisonType));
        }
        return isCorrectByComparison(compareResult, comparisonType);
    }

    private boolean isCorrectByComparison(int compareResult, ComparisonType comparisonType) {
        if (compareResult > 0) {
            return ComparisonType.GT == comparisonType || ComparisonType.GTE == comparisonType;
        } else if (compareResult < 0) {
            return ComparisonType.LT == comparisonType || ComparisonType.LTE == comparisonType;
        } else {
            return ComparisonType.GTE == comparisonType || ComparisonType.LTE == comparisonType;
        }
    }

    private Object getValue(Object tokenValue, ComparisonToken.ValueType valueType,
                            SecurityContext context, Map<String, String> arguments) {
        if (tokenValue == null) {
            return null;
        }
        if (ComparisonToken.ValueType.GRAPHQL_ARGUMENT_NAME == valueType) {
            if (arguments == null || arguments.isEmpty() || !(tokenValue instanceof String)) {
                return null;
            }
            return arguments.get(tokenValue);
        } else if (ComparisonToken.ValueType.GRAPHQL_CONTEXT_FIELD_NAME == valueType) {
            if (context == null) {
                return null;
            }
            Object value = context.get((String) tokenValue);
            if (value instanceof Enum<?>) {
                value = value.toString();
            }
            return value;
        } else if (ComparisonToken.ValueType.NULL == valueType) {
            return null;
        } else {
            return tokenValue;
        }
    }
}
