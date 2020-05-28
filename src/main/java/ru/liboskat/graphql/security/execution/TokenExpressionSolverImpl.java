package ru.liboskat.graphql.security.execution;

import ru.liboskat.graphql.security.storage.ComparisonToken;
import ru.liboskat.graphql.security.storage.OperatorToken;
import ru.liboskat.graphql.security.storage.Token;
import ru.liboskat.graphql.security.storage.TokenExpression;
import ru.liboskat.graphql.security.utils.NumberComparator;
import ru.liboskat.graphql.security.utils.TemporalComparator;

import java.time.temporal.Temporal;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An implementation of {@link TokenExpressionSolver} that is used to solve {@link TokenExpression}
 * in Reversed Polish Notation.
 */
public class TokenExpressionSolverImpl implements TokenExpressionSolver {
    private final TemporalComparator temporalComparator;
    private final NumberComparator numberComparator;

    /**
     * Creates new solver
     */
    public TokenExpressionSolverImpl() {
        this.temporalComparator = new TemporalComparator();
        this.numberComparator = new NumberComparator();
    }

    /**
     * Solves {@link TokenExpression} in Reversed Polish Notation
     * @param expression expression in Reversed Polish Notation
     * @param context security context of query
     * @param arguments arguments of queried field. May be null
     * @return result of solving the expression
     * @throws IllegalArgumentException if expression is incorrect
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
        if (token.getComparisonType() == ComparisonToken.ComparisonType.EQUALS) {
            if (firstValue instanceof Temporal && secondValue instanceof Temporal) {
                return temporalComparator.compare((Temporal) firstValue, (Temporal) secondValue) == 0;
            } else if (firstValue instanceof Number && secondValue instanceof Number) {
                return numberComparator.compare((Number) firstValue, (Number) secondValue) == 0;
            } else {
                return Objects.equals(firstValue, secondValue);
            }
        } else {
            int compareResult;
            if (firstValue instanceof Temporal && secondValue instanceof Temporal) {
                compareResult = temporalComparator.compare((Temporal) firstValue, (Temporal) secondValue);
            } else if (firstValue instanceof Number && secondValue instanceof Number) {
                compareResult = numberComparator.compare((Number) firstValue, (Number) secondValue);
            } else {
                throw new IllegalArgumentException(String.format("Can't compare %s and %s with operation %s",
                        firstValue, secondValue, token.getComparisonType()));
            }
            if (compareResult > 0) {
                return ComparisonToken.ComparisonType.GT == token.getComparisonType() ||
                        ComparisonToken.ComparisonType.GTE == token.getComparisonType();
            } else if (compareResult < 0) {
                return ComparisonToken.ComparisonType.LT == token.getComparisonType() ||
                        ComparisonToken.ComparisonType.LTE == token.getComparisonType();
            } else {
                return ComparisonToken.ComparisonType.GTE == token.getComparisonType() ||
                        ComparisonToken.ComparisonType.LTE == token.getComparisonType();
            }
        }
    }

    private Object getValue(Object tokenValue, ComparisonToken.ValueType valueType,
                            SecurityContext context, Map<String, String> arguments) {
        if (tokenValue == null) {
            return null;
        }
        if (ComparisonToken.ValueType.GRAPHQL_ARGUMENT_NAME == valueType) {
            if (arguments == null || arguments.isEmpty()) {
                return null;
            }
            return arguments.get((String) tokenValue);
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
