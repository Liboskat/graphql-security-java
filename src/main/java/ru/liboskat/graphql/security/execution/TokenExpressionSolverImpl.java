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
            //если токен - сравнение, кладем результат сравнения в стек
            if (token instanceof ComparisonToken) {
                stack.push(getResult((ComparisonToken) token, context, arguments));
            }
            /* если токен - оператор, берем необходимое число значений из стека,
            применяем к ним операцию, результат кладем обратно в стек */
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
            //результат на вершине стека
            result = stack.pop();
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("Illegal TokenExpression");
        }
        return result;
    }

    /**
     * Возвращает результат сравнения
     *
     * @param token     сравнение
     * @param ctx       контекст безопасности
     * @param arguments аргументы поля, могут быть null
     * @return результат сравнения
     */
    private boolean getResult(ComparisonToken token, SecurityContext ctx, Map<String, String> arguments) {
        Object firstValue = getValue(token.getFirstValue(), token.getFirstValueType(), ctx, arguments);
        Object secondValue = getValue(token.getSecondValue(), token.getSecondValueType(), ctx, arguments);
        if (token.getComparisonType() == ComparisonType.EQUALS) {
            return isEquals(firstValue, secondValue);
        } else {
            return compare(firstValue, secondValue, token.getComparisonType());
        }
    }

    /**
     * Сравнивает два объекта
     *
     * @param firstValue  первый объект
     * @param secondValue второй объект
     * @return true, если объекты равны, иначе false
     * @throws IllegalArgumentException, если ошибка при конвертации объекта
     */
    private boolean isEquals(Object firstValue, Object secondValue) {
        if (firstValue instanceof Number && secondValue instanceof Number) {
            //если числа, сравниваем double значения
            Number firstNumber = (Number) firstValue;
            Number secondNumber = (Number) secondValue;
            return firstNumber.doubleValue() == secondNumber.doubleValue();
        } else if (firstValue instanceof Temporal && secondValue instanceof Temporal) {
            //если дата / время, преобразуем в ZonedDateTime и сравниваем
            ZonedDateTime firstZonedDateTime = TemporalToZonedDateTimeConverter.convert((Temporal) firstValue);
            ZonedDateTime secondZonedDateTime = TemporalToZonedDateTimeConverter.convert((Temporal) secondValue);
            return firstZonedDateTime.equals(secondZonedDateTime);
        } else {
            //иначе делаем простой equals
            return Objects.equals(firstValue, secondValue);
        }
    }

    /**
     * Возвращает результат неравенства объектов в зависимости от оператора сравнения
     *
     * @param firstValue     первый объект (число или дата / время)
     * @param secondValue    второй объект (число или дата / время)
     * @param comparisonType тип сравнения
     * @return результат сравнения
     * @throws IllegalArgumentException, ошибка при конвертации объекта или невозможность сравнения переданных объектов
     */
    private boolean compare(Object firstValue, Object secondValue, ComparisonType comparisonType) {
        int compareResult;
        if (firstValue instanceof Temporal && secondValue instanceof Temporal) {
            //если дата / время, преобразуем в ZonedDateTime и получаем результат сравнения
            ZonedDateTime firstZonedDateTime = TemporalToZonedDateTimeConverter.convert((Temporal) firstValue);
            ZonedDateTime secondZonedDateTime = TemporalToZonedDateTimeConverter.convert((Temporal) secondValue);
            compareResult = firstZonedDateTime.compareTo(secondZonedDateTime);
        } else if (firstValue instanceof Number && secondValue instanceof Number) {
            //если числа, сравниваем double значения и получаем результат сравнения
            Number firstNumber = (Number) firstValue;
            Number secondNumber = (Number) secondValue;
            compareResult = Double.compare(firstNumber.doubleValue(), secondNumber.doubleValue());
        } else {
            //в любом другом случае выбрасываем исключение
            throw new IllegalArgumentException(String.format("Can't compare %s and %s with operation %s",
                    firstValue, secondValue, comparisonType));
        }
        //возвращаем результат в зависимости от результата сравнения и типа сравнения
        return isCorrectByComparison(compareResult, comparisonType);
    }

    /**
     * Возвращает булевый результат в зависимости от результата сравнения и типа сравнения
     *
     * @param compareResult  результат сравнения
     * @param comparisonType тип сравнения
     * @return булевый результат в зависимости от результата сравнения и типа сравнения
     */
    private boolean isCorrectByComparison(int compareResult, ComparisonType comparisonType) {
        if (compareResult > 0) {
            // если результат > 0: оператор - '>' или '>=' -> true, иначе false
            return ComparisonType.GT == comparisonType || ComparisonType.GTE == comparisonType;
        } else if (compareResult < 0) {
            // если результат < 0: оператор - '<' или '<=' -> true, иначе false
            return ComparisonType.LT == comparisonType || ComparisonType.LTE == comparisonType;
        } else {
            // если результат = 0: оператор - '<=' или '>=' -> true, иначе false
            return ComparisonType.GTE == comparisonType || ComparisonType.LTE == comparisonType;
        }
    }

    /**
     * Возвращает значение в зависимости от типа
     *
     * @param tokenValue объект
     * @param valueType  тип значения
     * @param context    контекст безопасности
     * @param arguments  аргументы поля
     * @return значение для дальнейшего сравнения
     */
    private Object getValue(Object tokenValue, ComparisonToken.ValueType valueType,
                            SecurityContext context, Map<String, String> arguments) {
        //если значение null - возвращаем null
        if (tokenValue == null) {
            return null;
        }
        if (ComparisonToken.ValueType.GRAPHQL_ARGUMENT_NAME == valueType) {
            //если аргументы пустые или название аргумента не String, тогда возвращаем null
            if (arguments == null || arguments.isEmpty() || !(tokenValue instanceof String)) {
                return null;
            }
            //возвращаем значение аргумента по названию
            return arguments.get(tokenValue);
        } else if (ComparisonToken.ValueType.GRAPHQL_CONTEXT_FIELD_NAME == valueType) {
            //если контекст null, возвращаем null
            if (context == null) {
                return null;
            }
            //возвращаем значение контекста по ключу
            Object value = context.get((String) tokenValue);
            if (value instanceof Enum<?>) {
                value = value.toString();
            }
            return value;
        } else if (ComparisonToken.ValueType.NULL == valueType) {
            //если тип сравнения NULL, возвращаем null
            return null;
        } else {
            //в любом другом случае возвращаем значение из токена
            return tokenValue;
        }
    }
}
