package ru.liboskat.graphql.security.expression.parsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liboskat.graphql.security.exceptions.InvalidExpressionException;
import ru.liboskat.graphql.security.storage.TokenExpression;
import ru.liboskat.graphql.security.storage.token.ComparisonToken;
import ru.liboskat.graphql.security.storage.token.ComparisonToken.ComparisonType;
import ru.liboskat.graphql.security.storage.token.ComparisonToken.ValueType;
import ru.liboskat.graphql.security.storage.token.OperatorToken;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static ru.liboskat.graphql.security.storage.token.OperatorToken.*;

/**
 * An implementation of {@link ExpressionParser} that is used to parse String access control expressions
 */
public class SimpleExpressionParser implements ExpressionParser {
    private static final Logger logger = LoggerFactory.getLogger(SimpleExpressionParser.class);

    private static final String IN_OPERATOR = "IN";
    private static final String NOT_IN_OPERATOR = "NOT IN";

    private ParsingState state;

    /**
     * Parses String access control expression
     *
     * @param expression String access control expression
     * @return {@link TokenExpression} in infix notation
     * @throws InvalidExpressionException if expression is invalid
     */
    @Override
    public TokenExpression parse(String expression) {
        logger.debug("Started parsing expression {}", expression);

        state = new ParsingState(expression);
        waitFirstOperandOrLeftParenthesesOrNegation();
        
        if (state.leftParenthesesCount != state.rightParenthesesCount) {
            throw new InvalidExpressionException(
                    String.format("%d parens is unclosed",
                            state.leftParenthesesCount - state.rightParenthesesCount), expression);
        }

        logger.debug("Ended parsing expression {}", expression);
        return state.result;
    }

    private void waitFirstOperandOrLeftParenthesesOrNegation() {
        Optional<Character> currentCharOptional = state.getCurrentChar();
        if (!currentCharOptional.isPresent()) {
            if (!state.result.isEmpty()) {
                throw new InvalidExpressionException("Illegal expression", state.expression);
            }
            return;
        }

        char currentChar = currentCharOptional.get();
        if (currentChar == '(') {
            state.leftParenthesesCount++;
            state.result.addToken(LEFT_PARENTHESIS);
            state.currentPosition++;
            waitFirstOperandOrLeftParenthesesOrNegation();
        } else if (currentChar == '!') {
            state.result.addToken(NOT);
            state.currentPosition++;
            waitLeftParenthesis();
        } else if (currentChar == ' ') {
            state.currentPosition++;
            waitFirstOperandOrLeftParenthesesOrNegation();
        } else if (currentChar == '\'') {
            state.leftOperandState = new OperandState();
            state.currentPosition++;
            readStringOperand(true);
        } else if (currentChar == '{') {
            state.leftOperandState = new OperandState();
            state.currentPosition++;
            readObjectOperand(true);
        } else if (currentChar == '$') {
            state.leftOperandState = new OperandState();
            state.currentPosition++;
            readGraphQLArgumentNameOperand(true);
        } else if (Character.isJavaIdentifierStart(currentChar)) {
            state.leftOperandState = new OperandState();
            readContextFieldNameOperand(true);
        } else {
            throw new InvalidExpressionException(currentChar, state.currentPosition,
                    "'(' / '!' / start of operand / ' '", state.expression);
        }
    }

    private void waitLeftParenthesis() {
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == '(') {
            state.leftParenthesesCount++;
            state.result.addToken(LEFT_PARENTHESIS);
            state.currentPosition++;
            waitFirstOperandOrLeftParenthesesOrNegation();
        } else {
            throw new InvalidExpressionException(currentChar, state.currentPosition, "'('",
                    state.expression);
        }
    }

    private void readStringOperand(boolean isLeft) {
        OperandState operandState = isLeft ? state.leftOperandState :
                state.rightOperandState;
        StringBuilder valueBuilder = operandState.valueBuilder;

        char currentChar = getCurrentCharOrElseThrow();
        if (!operandState.escapeNextChar && currentChar == '\'') {
            operandState.resultValueType = ValueType.STRING;
            operandState.resultValue = valueBuilder.toString();
            state.currentPosition++;
            endOperandReading(isLeft);
        } else if (!operandState.escapeNextChar && currentChar == '\\') {
            operandState.escapeNextChar = true;
            state.currentPosition++;
            readStringOperand(isLeft);
        } else {
            operandState.escapeNextChar = false;
            valueBuilder.append(currentChar);
            state.currentPosition++;
            readStringOperand(isLeft);
        }
    }

    private void readObjectOperand(boolean isLeft) {
        OperandState operandState = isLeft ? state.leftOperandState :
                state.rightOperandState;
        StringBuilder valueBuilder = operandState.valueBuilder;

        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == '}') {
            parseAndSaveObject(operandState);
            state.currentPosition++;
            endOperandReading(isLeft);
        } else {
            valueBuilder.append(currentChar);
            state.currentPosition++;
            readObjectOperand(isLeft);
        }
    }

    private void readGraphQLArgumentNameOperand(boolean isLeft) {
        readVariableNameOperand(isLeft, true);
    }

    private void readContextFieldNameOperand(boolean isLeft) {
        readVariableNameOperand(isLeft, false);
    }

    private void waitComparisonOperator() {
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == '!') {
            state.currentPosition++;
            readNotEquals();
        } else if (currentChar == '=') {
            state.operandComparisonType = ComparisonType.EQUALS;
            state.currentPosition++;
            waitRightOperand();
        } else if (currentChar == '<') {
            state.operandComparisonType = ComparisonType.LT;
            state.currentPosition++;
            readLteOrGte();
        } else if (currentChar == '>') {
            state.operandComparisonType = ComparisonType.GT;
            state.currentPosition++;
            readLteOrGte();
        } else if (currentChar == 'N' || currentChar == 'n') {
            state.inValuesState = new InValuesState();
            state.inValuesState.negated = true;
            checkInOperator();
        } else if (currentChar == 'I' || currentChar == 'i') {
            state.inValuesState = new InValuesState();
            state.inValuesState.negated = false;
            checkInOperator();
        } else if (currentChar == ' ') {
            state.currentPosition++;
            waitComparisonOperator();
        } else {
            throw new InvalidExpressionException(currentChar, state.currentPosition,
                    "'=' / '!=' / 'in' / 'not in' / ' '", state.expression);
        }
    }

    private void readNotEquals() {
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == '=') {
            state.result.addToken(NOT);
            state.operandComparisonType = ComparisonType.EQUALS;
            state.currentPosition++;
            waitRightOperand();
        } else {
            throw new InvalidExpressionException(currentChar, state.currentPosition, "'='",
                    state.expression);
        }
    }

    private void readLteOrGte() {
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == '=') {
            if (state.operandComparisonType == ComparisonType.LT) {
                state.operandComparisonType = ComparisonType.LTE;
            } else {
                state.operandComparisonType = ComparisonType.GTE;
            }
            state.currentPosition++;
        }
        waitRightOperand();
    }

    private void checkInOperator() {
        InValuesState inValuesState = state.inValuesState;
        String correctOperator = inValuesState.negated ? NOT_IN_OPERATOR : IN_OPERATOR;

        char currentChar = getCurrentCharOrElseThrow();
        if (inValuesState.operatorSymbolsRead < correctOperator.length() &&
                currentChar == correctOperator.charAt(inValuesState.operatorSymbolsRead)) {
            inValuesState.operatorSymbolsRead++;
            state.currentPosition++;
            checkInOperator();
        } else if (inValuesState.operatorSymbolsRead == correctOperator.length()) {
            state.currentPosition++;
            waitInValues();
        } else {
            throw new InvalidExpressionException(currentChar, state.currentPosition,
                    String.format("'%c'", correctOperator.charAt(inValuesState.operatorSymbolsRead)),
                    state.expression);
        }
    }

    private void waitInValues() {
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == '(') {
            state.currentPosition++;
            waitInValue();
        } else if (currentChar == ' ') {
            state.currentPosition++;
            waitInValues();
        } else {
            throw new InvalidExpressionException(currentChar, state.currentPosition, "'(' / ' '",
                    state.expression);
        }
    }

    private void waitInValue() {
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == '\'') {
            state.inValuesState.inValueBuilder = new StringBuilder();
            state.currentPosition++;
            readInValue();
        } else if (currentChar == ' ') {
            state.currentPosition++;
            waitInValue();
        } else {
            throw new InvalidExpressionException(currentChar, state.currentPosition, "'", state.expression);
        }
    }

    private void readInValue() {
        InValuesState inValuesState = state.inValuesState;
        StringBuilder inValueBuilder = inValuesState.inValueBuilder;

        char currentChar = getCurrentCharOrElseThrow();
        if (!inValuesState.escapeNextChar && currentChar == '\'') {
            inValuesState.inValues.add(inValueBuilder.toString());
            state.currentPosition++;
            waitCommaOrRightParenthesis();
        } else if (!inValuesState.escapeNextChar && currentChar == '\\') {
            inValuesState.escapeNextChar = true;
            state.currentPosition++;
            readInValue();
        } else {
            inValuesState.escapeNextChar = false;
            inValueBuilder.append(currentChar);
            state.currentPosition++;
            readInValue();
        }
    }

    private void waitCommaOrRightParenthesis() {
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == ',') {
            state.currentPosition++;
            waitInValue();
        } else if (currentChar == ')') {
            state.currentPosition++;
            endInReading();
        } else if (currentChar == ' ') {
            waitCommaOrRightParenthesis();
        } else {
            throw new InvalidExpressionException(currentChar, state.currentPosition, "',' / ')' / ' '",
                    state.expression);
        }
    }

    private void waitRightOperand() {
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == ' ') {
            state.currentPosition++;
            waitRightOperand();
        } else if (currentChar == '\'') {
            state.rightOperandState = new OperandState();
            state.currentPosition++;
            readStringOperand(false);
        } else if (currentChar == '{') {
            state.rightOperandState = new OperandState();
            state.currentPosition++;
            readObjectOperand(false);
        } else if (currentChar == '$') {
            state.rightOperandState = new OperandState();
            state.currentPosition++;
            readGraphQLArgumentNameOperand(false);
        } else if (Character.isJavaIdentifierStart(currentChar)) {
            state.rightOperandState = new OperandState();
            readContextFieldNameOperand(false);
        } else {
            throw new InvalidExpressionException(currentChar, state.currentPosition, "start of operand / ' '",
                    state.expression);
        }
    }

    private void waitCombiningOperatorOrRightParenthesis() {
        Optional<Character> currentCharOptional = state.getCurrentChar();
        if (!currentCharOptional.isPresent()) {
            return;
        }

        char currentChar = currentCharOptional.get();
        if (currentChar == ')') {
            state.rightParenthesesCount++;
            if (state.rightParenthesesCount > state.leftParenthesesCount) {
                throw new InvalidExpressionException("Matching left paren not found ", state.currentPosition,
                        state.expression);
            }
            state.result.addToken(OperatorToken.RIGHT_PARENTHESIS);
            state.currentPosition++;
            waitCombiningOperatorOrRightParenthesis();
        } else if (currentChar == '&') {
            state.result.addToken(AND);
            state.currentPosition++;
            waitFirstOperandOrLeftParenthesesOrNegation();
        } else if (currentChar == '|') {
            state.result.addToken(OR);
            state.currentPosition++;
            waitFirstOperandOrLeftParenthesesOrNegation();
        } else if (currentChar == ' ') {
            state.currentPosition++;
            waitCombiningOperatorOrRightParenthesis();
        } else {
            throw new InvalidExpressionException(currentChar, state.currentPosition, "')' / '&' / '|' / ' '",
                    state.expression);
        }
    }

    private void readVariableNameOperand(boolean isLeft, boolean isArgumentName) {
        OperandState operandState = isLeft ? state.leftOperandState :
                state.rightOperandState;
        StringBuilder nameBuilder = operandState.valueBuilder;

        Optional<Character> currentCharOptional = state.getCurrentChar();
        if (!currentCharOptional.isPresent() && !isLeft) {
            endVariableNameReading(isArgumentName, false);
            return;
        } else if (!currentCharOptional.isPresent()) {
            throw new InvalidExpressionException("Illegal expression", state.expression);
        }

        char currentChar = currentCharOptional.get();
        if (checkIsVariableNamePossibleCharacter(currentChar, isArgumentName, nameBuilder.length())) {
            nameBuilder.append(currentChar);
            state.currentPosition++;
            readVariableNameOperand(isLeft, isArgumentName);
        } else {
            if (nameBuilder.length() == 0) {
                throw new InvalidExpressionException("Context field name is empty", state.currentPosition,
                        state.expression);
            }
            endVariableNameReading(isArgumentName, isLeft);
        }
    }

    private boolean checkIsVariableNamePossibleCharacter(char character, boolean isArgumentName, int position) {
        if (isArgumentName) {
            return position == 0 && isGraphQLNameStart(character) ||
                    position > 0 && isGraphQLNamePart(character);
        } else {
            return position == 0 && Character.isJavaIdentifierStart(character) ||
                    position > 0 && Character.isJavaIdentifierPart(character);
        }
    }

    private void endVariableNameReading(boolean isArgumentName, boolean isLeft) {
        OperandState operandState = isLeft ? state.leftOperandState :
                state.rightOperandState;
        operandState.resultValue = operandState.valueBuilder.toString();
        operandState.resultValueType = isArgumentName ? ValueType.GRAPHQL_ARGUMENT_NAME :
                ValueType.GRAPHQL_CONTEXT_FIELD_NAME;
        endOperandReading(isLeft);
    }

    private void endOperandReading(boolean isLeft) {
        if (!isLeft) {
            addParsedOperandComparison();
            waitCombiningOperatorOrRightParenthesis();
        } else {
            waitComparisonOperator();
        }
    }

    private void endInReading() {
        InValuesState inValuesState = state.inValuesState;
        OperatorToken operator = inValuesState.negated ? AND : OR;
        boolean addParentheses = inValuesState.inValues.size() > 1;

        if (addParentheses)  {
            state.result.addToken(LEFT_PARENTHESIS);
        }
        inValuesState.inValues.stream().findFirst().ifPresent(this::addInValueComparison);
        inValuesState.inValues.stream().skip(1).forEachOrdered(inValue -> {
            state.result.addToken(operator);
            addInValueComparison(inValue);
        });
        if (addParentheses)  {
            state.result.addToken(RIGHT_PARENTHESIS);
        }
        waitCombiningOperatorOrRightParenthesis();
    }

    private void addInValueComparison(String inValue) {
        OperandState firstValue = state.leftOperandState;
        ComparisonToken comparisonToken = ComparisonToken.builder()
                .firstValue(firstValue.resultValue, firstValue.resultValueType)
                .secondValue(inValue, ValueType.STRING)
                .comparisonType(ComparisonType.EQUALS)
                .build();
        if (state.inValuesState.negated) {
            state.result.addToken(NOT);
        }
        state.result.addToken(comparisonToken);
    }

    private void parseAndSaveObject(OperandState readingObjectState) {
        String stringValue = readingObjectState.valueBuilder.toString().trim();
        if (stringValue.isEmpty()) {
            throw new InvalidExpressionException("Object is empty", state.currentPosition,
                    state.expression);
        }

        if ("null".equals(stringValue)) {
            readingObjectState.resultValue = ComparisonToken.NullValue.INSTANCE;
            readingObjectState.resultValueType = ValueType.NULL;
            return;
        }
        if ("true".equals(stringValue)) {
            readingObjectState.resultValue = Boolean.TRUE;
            readingObjectState.resultValueType = ValueType.BOOLEAN;
            return;
        }
        if ("false".equals(stringValue)) {
            readingObjectState.resultValue = Boolean.FALSE;
            readingObjectState.resultValueType = ValueType.BOOLEAN;
            return;
        }

        Optional<ZonedDateTime> zonedDateTime =
                tryParseDateTime(stringValue, ZonedDateTime::from, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        if (zonedDateTime.isPresent()) {
            readingObjectState.resultValue = zonedDateTime.get();
            readingObjectState.resultValueType = ValueType.ZONED_DATE_TIME;
            return;
        }
        Optional<LocalDateTime> localDateTime =
                tryParseDateTime(stringValue, LocalDateTime::from, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        if (localDateTime.isPresent()) {
            readingObjectState.resultValue = localDateTime.get();
            readingObjectState.resultValueType = ValueType.LOCAL_DATE_TIME;
            return;
        }
        Optional<LocalDate> localDate =
                tryParseDateTime(stringValue, LocalDate::from, DateTimeFormatter.ISO_LOCAL_DATE);
        if (localDate.isPresent()) {
            readingObjectState.resultValue = localDate.get();
            readingObjectState.resultValueType = ValueType.LOCAL_DATE;
            return;
        }
        Optional<LocalTime> localTime = tryParseDateTime(stringValue, LocalTime::from, DateTimeFormatter.ISO_LOCAL_TIME);
        if (localTime.isPresent()) {
            readingObjectState.resultValue = localTime.get();
            readingObjectState.resultValueType = ValueType.LOCAL_TIME;
            return;
        }

        Optional<Number> numberOptional = tryParseNumber(stringValue);
        if (numberOptional.isPresent()) {
            Number number = numberOptional.get();
            readingObjectState.resultValue = number;
            readingObjectState.resultValueType = getNumberValueType(number);
            return;
        }

        if (readingObjectState.resultValue == null) {
            throw new InvalidExpressionException("Object can't be parsed", state.currentPosition,
                    state.expression);
        }
    }

    private void addParsedOperandComparison() {
        OperandState firstValue = state.leftOperandState;
        OperandState secondValue = state.rightOperandState;
        ComparisonToken comparisonToken = ComparisonToken.builder()
                .firstValue(firstValue.resultValue, firstValue.resultValueType)
                .secondValue(secondValue.resultValue, secondValue.resultValueType)
                .comparisonType(state.operandComparisonType)
                .build();
        state.result.addToken(comparisonToken);
    }

    private char getCurrentCharOrElseThrow() {
        return state.getCurrentChar().orElseThrow(() ->
                new InvalidExpressionException("Illegal expression", state.expression));
    }

    private static boolean isGraphQLNameStart(char symbol) {
        return (symbol >= 'A' && symbol <= 'Z') || (symbol >= 'a' && symbol <= 'z') || symbol == '_';
    }

    private static boolean isGraphQLNamePart(char symbol) {
        return isGraphQLNameStart(symbol) || (symbol >= '0' && symbol <= '9');
    }

    private <T extends Temporal> Optional<T> tryParseDateTime(String value, TemporalQuery<T> temporalQuery,
                                                              DateTimeFormatter formatter) {
        try {
            return Optional.of(formatter.parse(value, temporalQuery));
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    private Optional<Number> tryParseNumber(String value) {
        ParsePosition position = new ParsePosition(0);
        Number number = NumberFormat.getInstance(Locale.US).parse(value, position);
        if (position.getIndex() != value.length()) {
            return Optional.empty();
        }
        return Optional.of(number);
    }

    private ValueType getNumberValueType(Number number) {
        if (number instanceof Long) {
            return ValueType.INTEGER;
        } else {
            return ValueType.REAL;
        }
    }

    private static class ParsingState {
        final String expression;
        final int expressionLength;
        int currentPosition;
        int leftParenthesesCount;
        int rightParenthesesCount;
        OperandState leftOperandState;
        OperandState rightOperandState;
        InValuesState inValuesState;
        ComparisonType operandComparisonType;
        TokenExpression result;

        ParsingState(String expression) {
            this.expression = expression;
            this.expressionLength = expression.length();
            this.currentPosition = 0;
            this.leftParenthesesCount = 0;
            this.rightParenthesesCount = 0;
            this.result = new TokenExpression();
        }

        Optional<Character> getCurrentChar() {
            if (currentPosition >= expressionLength) {
                return Optional.empty();
            }
            return Optional.of(expression.charAt(currentPosition));
        }
    }

    private static class OperandState {
        StringBuilder valueBuilder;
        Object resultValue;
        ValueType resultValueType;
        boolean escapeNextChar;

        public OperandState() {
            valueBuilder = new StringBuilder();
        }
    }

    private static class InValuesState {
        boolean negated;
        int operatorSymbolsRead;
        StringBuilder inValueBuilder;
        boolean escapeNextChar;
        List<String> inValues;

        public InValuesState() {
            inValues = new ArrayList<>();
        }
    }
}
