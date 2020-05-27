package ru.liboskat.graphql.security.expression.parsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liboskat.graphql.security.exceptions.InternalErrorException;
import ru.liboskat.graphql.security.exceptions.InvalidExpressionException;
import ru.liboskat.graphql.security.storage.ComparisonToken;
import ru.liboskat.graphql.security.storage.OperatorToken;
import ru.liboskat.graphql.security.storage.TokenExpression;
import ru.liboskat.graphql.security.storage.Token;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SimpleExpressionParser implements ExpressionParser {
    private static final Logger logger = LoggerFactory.getLogger(SimpleExpressionParser.class);

    private static final int STATE_WAITING_FIRST_OPERAND_OR_OPENING_BRACKET_OR_NEGATION = 0;
    private static final int STATE_WAITING_OPENING_BRACKET = 1;
    private static final int STATE_WAITING_COMPARISON_OPERATOR = 2;
    private static final int STATE_READING_NOT_EQUALS = 3;
    private static final int STATE_READING_LTE_OR_GTE = 4;
    private static final int STATE_WAITING_SECOND_OPERAND = 5;
    private static final int STATE_WAITING_IN_VALUES = 6;
    private static final int STATE_WAITING_IN_VALUE = 7;
    private static final int STATE_READING_IN_VALUES_STRING = 8;
    private static final int STATE_WAITING_COMMA_OR_BRACKET = 9;
    private static final int STATE_WAITING_COMBINING_OPERATOR_OR_CLOSING_BRACKET = 10;

    private static final String IN_OPERATOR = "IN";
    private static final String NOT_IN_OPERATOR = "NOT IN";

    private final ThreadLocal<Integer> startPosition;

    public SimpleExpressionParser() {
        startPosition = ThreadLocal.withInitial(() -> 0);
    }

    @Override
    public TokenExpression parse(String expression) {
        logger.debug("Started parsing expression {}", expression);

        int state = STATE_WAITING_FIRST_OPERAND_OR_OPENING_BRACKET_OR_NEGATION;
        Object firstValue = null;
        ComparisonToken.ValueType firstValueType = null;
        ComparisonToken.ComparisonType comparisonType = null;
        TokenExpression tokenExpression = new TokenExpression();
        int leftParenCount = 0;
        int rightParenCount = 0;
        boolean inNegated = false;
        boolean getIFromStartPosition;
        startPosition.set(0);
        int i = startPosition.get();
        char symbol;
        while (i < expression.length()) {
            getIFromStartPosition = false;
            symbol = expression.charAt(i);
            if (state == STATE_WAITING_FIRST_OPERAND_OR_OPENING_BRACKET_OR_NEGATION) {
                if (symbol == '$') {
                    startPosition.set(i + 1);
                    firstValue = readGraphQLArgumentName(expression);
                    firstValueType = ComparisonToken.ValueType.GRAPHQL_ARGUMENT_NAME;
                    state = STATE_WAITING_COMPARISON_OPERATOR;
                    getIFromStartPosition = true;
                } else if (symbol == '\'') {
                    startPosition.set(i + 1);
                    firstValue = readString(expression);
                    firstValueType = ComparisonToken.ValueType.STRING;
                    state = STATE_WAITING_COMPARISON_OPERATOR;
                    getIFromStartPosition = true;
                } else if (symbol == '{') {
                    startPosition.set(i + 1);
                    ValueAndTypeHolder valueAndTypeHolder = readObject(expression);
                    firstValue = valueAndTypeHolder.getValue();
                    firstValueType = valueAndTypeHolder.getValueType();
                    state = STATE_WAITING_COMPARISON_OPERATOR;
                    getIFromStartPosition = true;
                } else if (Character.isJavaIdentifierStart(symbol)) {
                    startPosition.set(i);
                    firstValue = readGraphQLContextFieldName(expression);
                    firstValueType = ComparisonToken.ValueType.GRAPHQL_CONTEXT_FIELD_NAME;
                    state = STATE_WAITING_COMPARISON_OPERATOR;
                    getIFromStartPosition = true;
                } else if (symbol == '!') {
                    tokenExpression.addToken(OperatorToken.NOT);
                    state = STATE_WAITING_OPENING_BRACKET;
                } else if (symbol == '(') {
                    leftParenCount++;
                    tokenExpression.addToken(OperatorToken.LEFT_PAREN);
                } else if (symbol != ' ') {
                    throw new InvalidExpressionException(symbol, i, "'(' / '!' / start of operand / ' '", expression);
                }
            } else if (state == STATE_WAITING_OPENING_BRACKET) {
                if (symbol == '(') {
                    leftParenCount++;
                    tokenExpression.addToken(OperatorToken.LEFT_PAREN);
                    state = STATE_WAITING_FIRST_OPERAND_OR_OPENING_BRACKET_OR_NEGATION;
                } else {
                    logger.error("");
                    throw new InvalidExpressionException(symbol, i, "'('", expression);
                }
            } else if (state == STATE_WAITING_COMPARISON_OPERATOR) {
                if (symbol == '!') {
                    state = STATE_READING_NOT_EQUALS;
                } else if (symbol == '=') {
                    comparisonType = ComparisonToken.ComparisonType.EQUALS;
                    state = STATE_WAITING_SECOND_OPERAND;
                } else if (symbol == '<') {
                    comparisonType = ComparisonToken.ComparisonType.LT;
                    state = STATE_READING_LTE_OR_GTE;
                } else if (symbol == '>') {
                    comparisonType = ComparisonToken.ComparisonType.GT;
                    state = STATE_READING_LTE_OR_GTE;
                } else if (symbol == 'N' || symbol == 'n') {
                    startPosition.set(i);
                    checkInExpression(expression, true);
                    inNegated = true;
                    getIFromStartPosition = true;
                    state = STATE_WAITING_IN_VALUES;
                } else if (symbol == 'I' || symbol == 'i') {
                    startPosition.set(i);
                    checkInExpression(expression, false);
                    inNegated = false;
                    getIFromStartPosition = true;
                    state = STATE_WAITING_IN_VALUES;
                } else if (symbol != ' ') {
                    throw new InvalidExpressionException(symbol, i, "'=' / '!=' / 'in' / 'not in' / ' '", expression);
                }
            } else if (state == STATE_READING_NOT_EQUALS) {
                if (symbol == '=') {
                    tokenExpression.addToken(OperatorToken.NOT);
                    comparisonType = ComparisonToken.ComparisonType.EQUALS;
                    state = STATE_WAITING_SECOND_OPERAND;
                } else {
                    throw new InvalidExpressionException(symbol, i, "'='", expression);
                }
            } else if (state == STATE_READING_LTE_OR_GTE) {
                if (symbol == '=' && comparisonType == ComparisonToken.ComparisonType.LT) {
                    comparisonType = ComparisonToken.ComparisonType.LTE;
                } else if (symbol == '=' && comparisonType == ComparisonToken.ComparisonType.GT) {
                    comparisonType = ComparisonToken.ComparisonType.GTE;
                } else {
                    startPosition.set(i);
                    getIFromStartPosition = true;
                }
                state = STATE_WAITING_SECOND_OPERAND;
            } else if (state == STATE_WAITING_IN_VALUES) {
                if (symbol == '(') {
                    startPosition.set(i + 1);
                    addInValues(tokenExpression, firstValue, firstValueType, readInValues(expression), inNegated);
                    getIFromStartPosition = true;
                    state = STATE_WAITING_COMBINING_OPERATOR_OR_CLOSING_BRACKET;
                } else if (symbol != ' ') {
                    throw new InvalidExpressionException(symbol, i, "'(' / ' '", expression);
                }
            } else if (state == STATE_WAITING_SECOND_OPERAND) {
                if (symbol == '$') {
                    startPosition.set(i + 1);
                    tokenExpression.addToken(ComparisonToken.builder()
                            .firstValue(firstValue, firstValueType)
                            .secondValue(readGraphQLArgumentName(expression), ComparisonToken.ValueType.GRAPHQL_ARGUMENT_NAME)
                            .comparisonType(comparisonType)
                            .build()
                    );
                    state = STATE_WAITING_COMBINING_OPERATOR_OR_CLOSING_BRACKET;
                    getIFromStartPosition = true;
                } else if (symbol == '\'') {
                    startPosition.set(i + 1);
                    tokenExpression.addToken(ComparisonToken.builder()
                            .firstValue(firstValue, firstValueType)
                            .secondValue(readString(expression), ComparisonToken.ValueType.STRING)
                            .comparisonType(comparisonType)
                            .build()
                    );
                    state = STATE_WAITING_COMBINING_OPERATOR_OR_CLOSING_BRACKET;
                    getIFromStartPosition = true;
                } else if (symbol == '{') {
                    startPosition.set(i + 1);
                    ValueAndTypeHolder valueAndTypeHolder = readObject(expression);
                    tokenExpression.addToken(ComparisonToken.builder()
                            .firstValue(firstValue, firstValueType)
                            .secondValue(valueAndTypeHolder.getValue(), valueAndTypeHolder.getValueType())
                            .comparisonType(comparisonType)
                            .build()
                    );
                    state = STATE_WAITING_COMBINING_OPERATOR_OR_CLOSING_BRACKET;
                    getIFromStartPosition = true;
                } else if (Character.isJavaIdentifierStart(symbol)) {
                    startPosition.set(i);
                    tokenExpression.addToken(ComparisonToken.builder()
                            .firstValue(firstValue, firstValueType)
                            .secondValue(readGraphQLContextFieldName(expression),
                                    ComparisonToken.ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                            .comparisonType(comparisonType)
                            .build()
                    );
                    state = STATE_WAITING_COMBINING_OPERATOR_OR_CLOSING_BRACKET;
                    getIFromStartPosition = true;
                } else if (symbol != ' ') {
                    throw new InvalidExpressionException(symbol, i, "start of operand / ' '", expression);
                }
            } else if (state == STATE_WAITING_COMBINING_OPERATOR_OR_CLOSING_BRACKET) {
                if (symbol == ')') {
                    rightParenCount++;
                    if (rightParenCount > leftParenCount) {
                        throw new InvalidExpressionException("Matching left paren not found ", i, expression);
                    }
                    tokenExpression.addToken(OperatorToken.RIGHT_PAREN);
                } else if (symbol == '&') {
                    tokenExpression.addToken(OperatorToken.AND);
                    state = STATE_WAITING_FIRST_OPERAND_OR_OPENING_BRACKET_OR_NEGATION;
                } else if (symbol == '|') {
                    tokenExpression.addToken(OperatorToken.OR);
                    state = STATE_WAITING_FIRST_OPERAND_OR_OPENING_BRACKET_OR_NEGATION;
                } else if (symbol != ' ') {
                    throw new InvalidExpressionException(symbol, i, "')' / '&' / '|' / ' '", expression);
                }
            } else {
                throw new InternalErrorException(String.format("Illegal state %d", state));
            }
            if (getIFromStartPosition) {
                i = startPosition.get();
            } else {
                i++;
            }
        }
        if (state != STATE_WAITING_COMBINING_OPERATOR_OR_CLOSING_BRACKET &&
                (!expression.isEmpty() || state != STATE_WAITING_FIRST_OPERAND_OR_OPENING_BRACKET_OR_NEGATION)) {
            throw new InvalidExpressionException("Illegal expression", expression);
        }
        if (leftParenCount != rightParenCount) {
            throw new InvalidExpressionException(
                    String.format("%d parens is unclosed", leftParenCount - rightParenCount), expression);
        }

        logger.debug("Ended parsing expression {}", expression);
        return tokenExpression;
    }

    private String readGraphQLArgumentName(String expression) {
        boolean valueReadingEnded = false;
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = startPosition.get(); i < expression.length() && !valueReadingEnded; i++) {
            char symbol = expression.charAt(i);
            if ((nameBuilder.length() == 0 && isGraphQLNameStart(symbol)) ||
                    (nameBuilder.length() > 0 && isGraphQLNamePart(symbol))) {
                nameBuilder.append(symbol);
            } else {
                startPosition.set(i);
                valueReadingEnded = true;
            }
        }
        String name = nameBuilder.toString();
        if (name.isEmpty()) {
            throw new InvalidExpressionException("Argument name is empty", startPosition.get(), expression);
        }
        if (!valueReadingEnded) {
            startPosition.set(expression.length());
        }
        return name;
    }

    private String readGraphQLContextFieldName(String expression) {
        boolean valueReadingEnded = false;
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = startPosition.get(); i < expression.length() && !valueReadingEnded; i++) {
            char symbol = expression.charAt(i);
            if ((nameBuilder.length() == 0 && Character.isJavaIdentifierStart(symbol)) ||
                    (nameBuilder.length() > 0 && Character.isJavaIdentifierPart(symbol))) {
                nameBuilder.append(symbol);
            } else {
                startPosition.set(i);
                valueReadingEnded = true;
            }
        }
        String name = nameBuilder.toString();
        if (name.isEmpty()) {
            throw new InvalidExpressionException("Context field name is empty", startPosition.get(), expression);
        }
        if (!valueReadingEnded) {
            startPosition.set(expression.length());
        }
        return name;
    }

    private String readString(String expression) {
        boolean valueReadingEnded = false;
        boolean escapeNext = false;
        StringBuilder name = new StringBuilder();
        for (int i = startPosition.get(); i < expression.length() && !valueReadingEnded; i++) {
            char symbol = expression.charAt(i);
            if (symbol == '\'' && !escapeNext) {
                startPosition.set(i + 1);
                valueReadingEnded = true;
            } else if (symbol == '\\' && !escapeNext) {
                escapeNext = true;
            } else {
                name.append(symbol);
                escapeNext = false;
            }
        }
        if (!valueReadingEnded) {
            throw new InvalidExpressionException("String is not closed", expression.length() - 1, expression);
        }
        return name.toString();
    }

    private ValueAndTypeHolder readObject(String expression) {
        boolean valueReadingEnded = false;
        StringBuilder stringValueBuilder = new StringBuilder();
        int beforeReadingPosition = startPosition.get();
        for (int i = beforeReadingPosition; i < expression.length() && !valueReadingEnded; i++) {
            char symbol = expression.charAt(i);
            if (symbol == '}') {
                startPosition.set(i + 1);
                valueReadingEnded = true;
            } else {
                stringValueBuilder.append(symbol);
            }
        }
        if (!valueReadingEnded) {
            throw new InvalidExpressionException("Object is not closed", expression.length() - 1, expression);
        }
        String stringValue = stringValueBuilder.toString().trim();
        if (stringValue.isEmpty()) {
            throw new InvalidExpressionException("Object is empty", beforeReadingPosition, expression);
        }
        if ("null".equals(stringValue)) {
            return new ValueAndTypeHolder(ComparisonToken.NullValue.INSTANCE, ComparisonToken.ValueType.NULL);
        }
        if ("true".equals(stringValue)) {
            return new ValueAndTypeHolder(Boolean.TRUE, ComparisonToken.ValueType.BOOLEAN);
        }
        if ("false".equals(stringValue)) {
            return new ValueAndTypeHolder(Boolean.FALSE, ComparisonToken.ValueType.BOOLEAN);
        }
        Optional<ValueAndTypeHolder> temporalOptional = tryGetTemporal(stringValue);
        if (temporalOptional.isPresent()) {
            return temporalOptional.get();
        }
        Optional<ValueAndTypeHolder> numberOptional = tryGetNumber(stringValue);
        if (numberOptional.isPresent()) {
            return numberOptional.get();
        }
        throw new InvalidExpressionException("Object can't be parsed", beforeReadingPosition, expression);
    }

    private void checkInExpression(String expression, boolean negated) {
        String operator = negated ? NOT_IN_OPERATOR : IN_OPERATOR;
        boolean operatorReadingEnded = false;
        int symbolsRead = 0;
        for (int i = startPosition.get(); i < expression.length() && !operatorReadingEnded; i++) {
            char symbol = Character.toUpperCase(expression.charAt(i));
            if (symbolsRead < operator.length() && symbol == operator.charAt(symbolsRead)) {
                symbolsRead++;
            } else if (symbolsRead == operator.length()) {
                startPosition.set(i);
                operatorReadingEnded = true;
            } else {
                throw new InvalidExpressionException(
                        symbol, i, String.format("'%c'", operator.charAt(symbolsRead)), expression);
            }
        }
        if (!operatorReadingEnded) {
            throw new InvalidExpressionException(
                    String.format("Incomplete %s expression", operator), expression.length() - 1, expression);
        }
    }

    private Set<String> readInValues(String expression) {
        Set<String> inValues = new HashSet<>();
        int state = STATE_WAITING_IN_VALUE;
        boolean readingEnded = false;
        StringBuilder value = new StringBuilder();
        for (int i = startPosition.get(); i < expression.length() && !readingEnded; i++) {
            char symbol = expression.charAt(i);
            if (state == STATE_WAITING_IN_VALUE) {
                if (symbol == '\'') {
                    state = STATE_READING_IN_VALUES_STRING;
                } else if (symbol != ' ') {
                    throw new InvalidExpressionException(symbol, i, "\' / ' '", expression);
                }
            } else if (state == STATE_READING_IN_VALUES_STRING) {
                if (symbol == '\'') {
                    inValues.add(value.toString());
                    value = new StringBuilder();
                    state = STATE_WAITING_COMMA_OR_BRACKET;
                } else {
                    value.append(symbol);
                }
            } else if (state == STATE_WAITING_COMMA_OR_BRACKET) {
                if (symbol == ',') {
                    state = STATE_WAITING_IN_VALUE;
                } else if (symbol == ')') {
                    startPosition.set(i + 1);
                    readingEnded = true;
                } else if (symbol != ' ') {
                    throw new InvalidExpressionException(symbol, i, "',' / ')' / ' '", expression);
                }
            } else {
                throw new InternalErrorException(String.format("Illegal state %d", state));
            }
        }
        if (!readingEnded) {
            throw new InvalidExpressionException("Values didn't closed", expression.length() - 1, expression);
        }
        return inValues;
    }

    private void addInValues(TokenExpression tokenExpression, Object firstValue, ComparisonToken.ValueType firstValueType,
                             Set<String> inValues, boolean negated) {
        List<Token> equalityTokens = inValues.stream()
                .map(inValue -> ComparisonToken.builder()
                        .firstValue(firstValue, firstValueType)
                        .secondValue(inValue, ComparisonToken.ValueType.STRING)
                        .build())
                .collect(Collectors.toList());
        boolean addParentheses = equalityTokens.size() > 1;
        if (addParentheses) {
            tokenExpression.addToken(OperatorToken.LEFT_PAREN);
        }
        equalityTokens.stream()
                .findFirst()
                .ifPresent(token -> {
                    if (negated) {
                        tokenExpression.addToken(OperatorToken.NOT);
                    }
                    tokenExpression.addToken(token);
                });
        equalityTokens.stream()
                .skip(1)
                .forEach(token -> {
                    if (negated) {
                        tokenExpression.addToken(OperatorToken.AND);
                        tokenExpression.addToken(OperatorToken.NOT);
                    } else {
                        tokenExpression.addToken(OperatorToken.OR);
                    }
                    tokenExpression.addToken(token);
                });
        if (addParentheses) {
            tokenExpression.addToken(OperatorToken.RIGHT_PAREN);
        }
    }

    private static boolean isGraphQLNameStart(char symbol) {
        return (symbol >= 'A' && symbol <= 'Z') || (symbol >= 'a' && symbol <= 'z') || symbol == '_';
    }

    private static boolean isGraphQLNamePart(char symbol) {
        return isGraphQLNameStart(symbol) || (symbol >= '0' && symbol <= '9');
    }

    private Optional<ValueAndTypeHolder> tryGetTemporal(String value) {
        try {
            return Optional.of(ZonedDateTime.parse(value))
                    .map(zonedDateTime -> new ValueAndTypeHolder(zonedDateTime, ComparisonToken.ValueType.ZONED_DATE_TIME));
        } catch (DateTimeParseException ignored) { }
        try {
            return Optional.of(LocalDateTime.parse(value))
                    .map(localDateTime -> new ValueAndTypeHolder(localDateTime, ComparisonToken.ValueType.LOCAL_DATE_TIME));
        } catch (DateTimeParseException ignored) { }
        try {
            return Optional.of(LocalDate.parse(value))
                    .map(localDate -> new ValueAndTypeHolder(localDate, ComparisonToken.ValueType.LOCAL_DATE));
        } catch (DateTimeParseException ignored) { }
        try {
            return Optional.of(LocalTime.parse(value))
                    .map(localTime -> new ValueAndTypeHolder(localTime, ComparisonToken.ValueType.LOCAL_TIME));
        } catch (DateTimeParseException ignored) { }
        return Optional.empty();
    }

    private Optional<ValueAndTypeHolder> tryGetNumber(String value) {
        try {
            return Optional.of(Integer.parseInt(value))
                    .map(intValue -> new ValueAndTypeHolder(intValue, ComparisonToken.ValueType.INTEGER));
        } catch (NumberFormatException ignored) { }
        try {
            return Optional.of(Long.parseLong(value))
                    .map(longValue -> new ValueAndTypeHolder(longValue, ComparisonToken.ValueType.LONG));
        } catch (NumberFormatException ignored) { }
        try {
            return Optional.of(Double.parseDouble(value))
                    .map(doubleValue -> new ValueAndTypeHolder(doubleValue, ComparisonToken.ValueType.DOUBLE));
        } catch (NumberFormatException ignored) { }
        return Optional.empty();
    }

    private static class ValueAndTypeHolder {
        private final Object value;
        private final ComparisonToken.ValueType valueType;

        private ValueAndTypeHolder(Object value, ComparisonToken.ValueType valueType) {
            this.value = value;
            this.valueType = valueType;
        }

        private Object getValue() {
            return value;
        }

        private ComparisonToken.ValueType getValueType() {
            return valueType;
        }
    }
}
