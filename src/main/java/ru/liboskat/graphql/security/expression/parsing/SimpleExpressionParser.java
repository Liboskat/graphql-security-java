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
 * Реализация {@link ExpressionParser}, используемая для парсинга строковых выражений в объектный вид с инфиксной записью
 */
public class SimpleExpressionParser implements ExpressionParser {
    private static final Logger logger = LoggerFactory.getLogger(SimpleExpressionParser.class);

    private static final String IN_OPERATOR = "IN";
    private static final String NOT_IN_OPERATOR = "NOT IN";

    private ParsingState state;

    /**
     * Метод парсинга строковых выражений
     *
     * @param expression строковое выражение в инфиксной записи
     * @return {@link TokenExpression} выражение в объектном виде в инфиксной записи
     * @throws InvalidExpressionException если выражение - некорректное
     */
    @Override
    public TokenExpression parse(String expression) {
        logger.debug("Started parsing expression {}", expression);

        /* создаем новое состояние парсинга, начинаем парсинг с ожидания первого операнда или
        открывающих скобок с возможным отрицанием */
        state = new ParsingState(expression);
        waitLeftOperandOrLeftParenthesesOrNegation();

        // проверяем выражение на соответствие количества открывающих и закрывающих скобок
        if (state.leftParenthesesCount != state.rightParenthesesCount) {
            throw new InvalidExpressionException(
                    String.format("%d parens is unclosed",
                            state.leftParenthesesCount - state.rightParenthesesCount), expression);
        }

        logger.debug("Ended parsing expression {}", expression);

        //возвращаем результат из состояния
        return state.result;
    }

    /**
     * В этом методе происходит ожидание первого оператора или открывающих скобок с возможным отрицанием
     */
    private void waitLeftOperandOrLeftParenthesesOrNegation() {
        Optional<Character> currentCharOptional = state.getCurrentChar();
        // если символы закончились, а выражение не пустое, выбрасываем исключение
        if (!currentCharOptional.isPresent()) {
            if (!state.result.isEmpty()) {
                throw new InvalidExpressionException("Illegal expression", state.expression);
            }
            return;
        }

        char currentChar = currentCharOptional.get();
        if (currentChar == '(') {
            //если символ - открывающая скобка, добавляем скобку в выражение, остаемся в этом методе
            state.leftParenthesesCount++;
            state.result.addToken(LEFT_PARENTHESIS);
            state.currentPosition++;
            waitLeftOperandOrLeftParenthesesOrNegation();
        } else if (currentChar == '!') {
            //если символ - отрицание, добавляем отрицание в выражение, переходим в метод ожидания открывающей скобки
            state.result.addToken(NOT);
            state.currentPosition++;
            waitLeftParenthesis();
        } else if (currentChar == ' ') {
            //если символ - пробел, остаемся в этом методе
            state.currentPosition++;
            waitLeftOperandOrLeftParenthesesOrNegation();
        } else if (currentChar == '\'') {
            //если символ - одинарная кавычка, переходим в метод прочтения строки
            state.leftOperandState = new OperandState();
            state.currentPosition++;
            readStringOperand(true);
        } else if (currentChar == '{') {
            //если символ - {, переходим в метод прочтения объекта
            state.leftOperandState = new OperandState();
            state.currentPosition++;
            readObjectOperand(true);
        } else if (currentChar == '$') {
            //если символ - $, переходим в метод прочтения GraphQL аргумента
            state.leftOperandState = new OperandState();
            state.currentPosition++;
            readGraphQLArgumentNameOperand(true);
        } else if (Character.isJavaIdentifierStart(currentChar)) {
            //если символ - начало идентификатора Java, переходим в метод прочтения поля контекста, начиная с текущего символа
            state.leftOperandState = new OperandState();
            readContextFieldNameOperand(true);
        } else {
            //если символ не является ни одним из вышеперечисленных, выбрасываем исключение
            throw new InvalidExpressionException(currentChar, state.currentPosition,
                    "'(' / '!' / start of operand / ' '", state.expression);
        }
    }

    /**
     * В этом методе происходит ожидание открывающей скобки
     */
    private void waitLeftParenthesis() {
        //если символа нет, выбрасываем исключение
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == '(') {
            //если символ - открывающая скобка, возвращаемся в метод waitLeftOperandOrLeftParenthesesOrNegation()
            state.leftParenthesesCount++;
            state.result.addToken(LEFT_PARENTHESIS);
            state.currentPosition++;
            waitLeftOperandOrLeftParenthesesOrNegation();
        } else {
            //если символ не является открывающей скобкой, выбрасываем исключение
            throw new InvalidExpressionException(currentChar, state.currentPosition, "'('",
                    state.expression);
        }
    }

    /**
     * В этом методе происходит чтение строки
     *
     * @param isLeft находится ли строка слева в сравнении
     */
    private void readStringOperand(boolean isLeft) {
        //получаем состояние операнда в зависимости от нахождения в сравнении
        OperandState operandState = isLeft ? state.leftOperandState :
                state.rightOperandState;
        StringBuilder valueBuilder = operandState.valueBuilder;

        //если символа нет, выбрасываем исключение
        char currentChar = getCurrentCharOrElseThrow();
        if (!operandState.escapeNextChar && currentChar == '\'') {
            //если символ кавычка и он не экранирован, заканчиваем чтение операнда
            operandState.resultValueType = ValueType.STRING;
            operandState.resultValue = valueBuilder.toString();
            state.currentPosition++;
            endOperandReading(isLeft);
        } else if (!operandState.escapeNextChar && currentChar == '\\') {
            //если символ - '\' и он не экранирован, экранируем следующий символ, остаемся в текущем методе
            operandState.escapeNextChar = true;
            state.currentPosition++;
            readStringOperand(isLeft);
        } else {
            //в любом другом случае отменяем экранирование, добавляем символ в StringBuilder, остаемся в текущем методе
            operandState.escapeNextChar = false;
            valueBuilder.append(currentChar);
            state.currentPosition++;
            readStringOperand(isLeft);
        }
    }

    /**
     * В этом методе происходит чтение объекта
     *
     * @param isLeft находится ли объект слева в сравнении
     */
    private void readObjectOperand(boolean isLeft) {
        //получаем состояние операнда в зависимости от нахождения в сравнении
        OperandState operandState = isLeft ? state.leftOperandState :
                state.rightOperandState;
        StringBuilder valueBuilder = operandState.valueBuilder;

        //если символа нет, выбрасываем исключение
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == '}') {
            //если символ - }, парсим объект, заканчиваем чтение операнда
            parseAndSaveObject(operandState);
            state.currentPosition++;
            endOperandReading(isLeft);
        } else {
            //в любом другом случае добавляем символ в StringBuilder, остаемся в текущем методе
            valueBuilder.append(currentChar);
            state.currentPosition++;
            readObjectOperand(isLeft);
        }
    }

    /**
     * В этом методе происходит чтение названия аргумента поля GraphQL

     * @param isLeft находится ли название аргумента слева в сравнении
     */
    private void readGraphQLArgumentNameOperand(boolean isLeft) {
        readVariableNameOperand(isLeft, true);
    }

    /**
     * В этом методе происходит чтение названия поля контекста
     *
     * @param isLeft находится ли название поля контекста слева в сравнении
     */
    private void readContextFieldNameOperand(boolean isLeft) {
        readVariableNameOperand(isLeft, false);
    }

    /**
     * В этом методе происходит ожидание оператора сравнения
     */
    private void waitComparisonOperator() {
        //если символа нет, выбрасываем исключение
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == '!') {
            //если символ - !, переходим в метод чтения оператора "не равно"
            state.currentPosition++;
            readNotEquals();
        } else if (currentChar == '=') {
            //если символ - =, запоминаем тип сравнения - равно, переходим в метод ожидания второго операнда
            state.operandComparisonType = ComparisonType.EQUALS;
            state.currentPosition++;
            waitRightOperand();
        } else if (currentChar == '<') {
            //если символ - <, запоминаем тип сравнения - меньше, переходим в метод чтения меньше или равно
            state.operandComparisonType = ComparisonType.LT;
            state.currentPosition++;
            readLteOrGte();
        } else if (currentChar == '>') {
            //если символ - >, запоминаем тип сравнения - больше, переходим в метод чтения больше или равно
            state.operandComparisonType = ComparisonType.GT;
            state.currentPosition++;
            readLteOrGte();
        } else if (currentChar == 'N' || currentChar == 'n') {
            /* если символ - N или n, создаем новое состояние чтения IN, сохраняем информацию об отрицании IN,
               переходим в метод проверки оператора IN / NOT IN */
            state.inValuesState = new InValuesState();
            state.inValuesState.negated = true;
            checkInOperator();
        } else if (currentChar == 'I' || currentChar == 'i') {
            /* если символ - I или i, создаем новое состояние чтения IN,
               переходим в метод проверки оператора IN / NOT IN */
            state.inValuesState = new InValuesState();
            state.inValuesState.negated = false;
            checkInOperator();
        } else if (currentChar == ' ') {
            //если символ - пробел, остаемся в текущем методе
            state.currentPosition++;
            waitComparisonOperator();
        } else {
            //в любом другом случае выбрасываем исключение
            throw new InvalidExpressionException(currentChar, state.currentPosition,
                    "'=' / '!=' / 'in' / 'not in' / ' '", state.expression);
        }
    }

    /**
     * В этом методе происходит чтение оператора "не равно"
     */
    private void readNotEquals() {
        //если символа нет, выбрасываем исключение
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == '=') {
            //если символ - '=', добавляем отрицание в выражение, сохраняем тип сравнения - равно
            state.result.addToken(NOT);
            state.operandComparisonType = ComparisonType.EQUALS;
            state.currentPosition++;
            waitRightOperand();
        } else {
            //в любом другом случае выбрасываем исключение
            throw new InvalidExpressionException(currentChar, state.currentPosition, "'='",
                    state.expression);
        }
    }

    /**
     * В этом методе происходит чтение больше или равно / меньше или равно
     */
    private void readLteOrGte() {
        //если символа нет, выбрасываем исключение
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == '=') {
            //если символ - '=', меняем меньше и больше на меньше или равно и больше или равно соответственно
            if (state.operandComparisonType == ComparisonType.LT) {
                state.operandComparisonType = ComparisonType.LTE;
            } else {
                state.operandComparisonType = ComparisonType.GTE;
            }
            state.currentPosition++;
        }
        //если символ - не '=', оставляем операторы сравнения неизменными, в следующем методе начинаем читать с текущего символа
        //переходим в метод ожидания операнда, находящегося справа в сравнении
        waitRightOperand();
    }

    /**
     * В этом методе происходит проверка корректности именования операторов IN / NOT IN
     */
    private void checkInOperator() {
        InValuesState inValuesState = state.inValuesState;
        //берем корректное название оператора в зависимости от отрицания IN
        String correctOperator = inValuesState.negated ? NOT_IN_OPERATOR : IN_OPERATOR;

        //если символа нет, выбрасываем исключение
        char currentChar = getCurrentCharOrElseThrow();
        if (inValuesState.operatorSymbolsRead < correctOperator.length() &&
                currentChar == correctOperator.charAt(inValuesState.operatorSymbolsRead)) {
            /* если количество проверенных символов меньше длины строки корректного названия оператора и
               проверяемый символ равен соответсвующему в корректном названии оператора,
               остаемся в методе для проверки следующего символа */
            inValuesState.operatorSymbolsRead++;
            state.currentPosition++;
            checkInOperator();
        } else if (inValuesState.operatorSymbolsRead == correctOperator.length()) {
            /* если количество проверенных символов соответсвует длине строки корректного названия оператора, переходим в
               метод ожидания значений IN */
            state.currentPosition++;
            waitInValues();
        } else {
            //в любом другом случае выбрасываем исключение
            throw new InvalidExpressionException(currentChar, state.currentPosition,
                    String.format("'%c'", correctOperator.charAt(inValuesState.operatorSymbolsRead)),
                    state.expression);
        }
    }

    /**
     * Метод, в котором происходит ожидание значений операторов IN / NOT IN
     */
    private void waitInValues() {
        //если символа нет, выбрасываем исключение
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == '(') {
            //если символ - (, переходим в метод ожидания конкретного значения IN / NOT IN
            state.currentPosition++;
            waitInValue();
        } else if (currentChar == ' ') {
            //если символ - пробел, остаемся в методе
            state.currentPosition++;
            waitInValues();
        } else {
            //в любом другом случае выбрасываем исключение
            throw new InvalidExpressionException(currentChar, state.currentPosition, "'(' / ' '",
                    state.expression);
        }
    }

    /**
     * В этом методе происходит ожидание конкретного значения IN / NOT IN
     */
    private void waitInValue() {
        //если символа нет, выбрасываем исключение
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == '\'') {
            //если символ - ', создаем новый StringBuilder для строки, переходим в состояние чтения значения IN / NOT IN
            state.inValuesState.inValueBuilder = new StringBuilder();
            state.currentPosition++;
            readInValue();
        } else if (currentChar == ' ') {
            //если символ - пробел, остаемся в методе
            state.currentPosition++;
            waitInValue();
        } else {
            //в любом другом случае выбрасываем исключение
            throw new InvalidExpressionException(currentChar, state.currentPosition, "'", state.expression);
        }
    }

    /**
     * В этом методе происходит чтение конкретного значения IN / NOT IN
     */
    private void readInValue() {
        InValuesState inValuesState = state.inValuesState;
        StringBuilder inValueBuilder = inValuesState.inValueBuilder;

        //если символа нет, выбрасываем исключение
        char currentChar = getCurrentCharOrElseThrow();
        if (!inValuesState.escapeNextChar && currentChar == '\'') {
            /* если символ кавычка и он не экранирован, заканчиваем чтение значения,
            переходим в метод ожидания запятой или закрывающей скобки
            */
            inValuesState.inValues.add(inValueBuilder.toString());
            state.currentPosition++;
            waitCommaOrRightParenthesis();
        } else if (!inValuesState.escapeNextChar && currentChar == '\\') {
            //если символ - '\' и он не экранирован, экранируем следующий символ, остаемся в текущем методе
            inValuesState.escapeNextChar = true;
            state.currentPosition++;
            readInValue();
        } else {
            //в любом другом случае отменяем экранирование, добавляем символ в StringBuilder, остаемся в текущем методе
            inValuesState.escapeNextChar = false;
            inValueBuilder.append(currentChar);
            state.currentPosition++;
            readInValue();
        }
    }

    /**
     * В этом методе происходит ожидание запятой или закрывающей скобки
     */
    private void waitCommaOrRightParenthesis() {
        //если символа нет, выбрасываем исключение
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == ',') {
            //если символ - запятая, переходим в состояние ожидания конкретного значения IN / NOT IN
            state.currentPosition++;
            waitInValue();
        } else if (currentChar == ')') {
            //если символ - закрывающая скобка, заканчиваем чтение значений IN / NOT IN
            state.currentPosition++;
            endInReading();
        } else if (currentChar == ' ') {
            //если символ - пробел, остаемся в текущем методе
            state.currentPosition++;
            waitCommaOrRightParenthesis();
        } else {
            //в любом другом случае выбрасываем исключение
            throw new InvalidExpressionException(currentChar, state.currentPosition, "',' / ')' / ' '",
                    state.expression);
        }
    }

    /**
     * В этом методе происходит ожидание операнда, находящего справа в сравнении
     */
    private void waitRightOperand() {
        //если символа нет, выбрасываем исключение
        char currentChar = getCurrentCharOrElseThrow();
        if (currentChar == ' ') {
            //если символ - пробел, остаемся в текущем методе
            state.currentPosition++;
            waitRightOperand();
        } else if (currentChar == '\'') {
            //если символ - одинарная кавычка, переходим в метод прочтения строки
            state.rightOperandState = new OperandState();
            state.currentPosition++;
            readStringOperand(false);
        } else if (currentChar == '{') {
            //если символ - {, переходим в метод прочтения объекта
            state.rightOperandState = new OperandState();
            state.currentPosition++;
            readObjectOperand(false);
        } else if (currentChar == '$') {
            //если символ - $, переходим в метод прочтения GraphQL аргумента
            state.rightOperandState = new OperandState();
            state.currentPosition++;
            readGraphQLArgumentNameOperand(false);
        } else if (Character.isJavaIdentifierStart(currentChar)) {
            //если символ - начало идентификатора Java, переходим в метод прочтения поля контекста, начиная с текущего символа
            state.rightOperandState = new OperandState();
            readContextFieldNameOperand(false);
        } else {
            //в любом другом случае выбрасываем исключение
            throw new InvalidExpressionException(currentChar, state.currentPosition, "start of operand / ' '",
                    state.expression);
        }
    }

    /**
     * В этом методе происходит ожидание закрывающей скобки или оператора комбинации сравнений
     */
    private void waitCombiningOperatorOrRightParenthesis() {
        //если символа нет, выходим из метода
        Optional<Character> currentCharOptional = state.getCurrentChar();
        if (!currentCharOptional.isPresent()) {
            return;
        }

        char currentChar = currentCharOptional.get();
        if (currentChar == ')') {
            /* если символ - ')', остаемся в текущем методе, добавляем закрывающую скобку в выражение,
            проверяем корректность соответствия открывающих и закрывающих скобок */
            state.rightParenthesesCount++;
            if (state.rightParenthesesCount > state.leftParenthesesCount) {
                throw new InvalidExpressionException("Matching left paren not found ", state.currentPosition,
                        state.expression);
            }
            state.result.addToken(OperatorToken.RIGHT_PARENTHESIS);
            state.currentPosition++;
            waitCombiningOperatorOrRightParenthesis();
        } else if (currentChar == '&') {
            /* если символ - '&', переходим в метод waitLeftOperandOrLeftParenthesesOrNegation(),
            добавляем оператор И в выражение */
            state.result.addToken(AND);
            state.currentPosition++;
            waitLeftOperandOrLeftParenthesesOrNegation();
        } else if (currentChar == '|') {
            /* если символ - '|', переходим в метод waitLeftOperandOrLeftParenthesesOrNegation(),
            добавляем оператор ИЛИ в выражение */
            state.result.addToken(OR);
            state.currentPosition++;
            waitLeftOperandOrLeftParenthesesOrNegation();
        } else if (currentChar == ' ') {
            //если символ - пробел, остаемся в текущем методе
            state.currentPosition++;
            waitCombiningOperatorOrRightParenthesis();
        } else {
            //в любом другом случае выбрасываем исключение
            throw new InvalidExpressionException(currentChar, state.currentPosition, "')' / '&' / '|' / ' '",
                    state.expression);
        }
    }

    /**
     * В этом методе происходит чтение названий аргумента и переменной контекста
     * @param isLeft находится ли операнд слева от сравнения
     * @param isArgumentName является ли операнд названием аргумента
     */
    private void readVariableNameOperand(boolean isLeft, boolean isArgumentName) {
        //получаем состояние операнда в зависимости от нахождения в сравнении
        OperandState operandState = isLeft ? state.leftOperandState :
                state.rightOperandState;
        StringBuilder nameBuilder = operandState.valueBuilder;

        /* если символа нет и оператор находится слева от сравнения, выбрасываем исключение,
        если символа нет и оператор - справа, заканчиваем чтение названия переменной и выходим из метода*/
        Optional<Character> currentCharOptional = state.getCurrentChar();
        if (!currentCharOptional.isPresent() && !isLeft) {
            endVariableNameReading(isArgumentName, false);
            return;
        } else if (!currentCharOptional.isPresent()) {
            throw new InvalidExpressionException("Illegal expression", state.expression);
        }

        char currentChar = currentCharOptional.get();
        if (checkIsVariableNamePossibleCharacter(currentChar, isArgumentName, nameBuilder.length())) {
            //если символ является возможным для названия, добавляем его в StringBuilder, остаемся в методе
            nameBuilder.append(currentChar);
            state.currentPosition++;
            readVariableNameOperand(isLeft, isArgumentName);
        } else {
            /* в любом другом случае, если прочтенное название - пустое, выбрасываем ошибку,
            иначе заканчиваем чтение названия переменной */
            if (nameBuilder.length() == 0) {
                throw new InvalidExpressionException("Context field name is empty", state.currentPosition,
                        state.expression);
            }
            endVariableNameReading(isArgumentName, isLeft);
        }
    }

    /**
     * В этом методе происходит проверка: является ли символ возможным для названия аргумента или контекста
     * @param character символ
     * @param isArgumentName является ли операнд названием аргумента
     * @param position позиция в названии операнда
     * @return возможность наличия символа в названии
     */
    private boolean checkIsVariableNamePossibleCharacter(char character, boolean isArgumentName, int position) {
        if (isArgumentName) {
            // для операнда название аргумента - правила именования GraphQL
            return position == 0 && isGraphQLNameStart(character) ||
                    position > 0 && isGraphQLNamePart(character);
        } else {
            // для операнда название переменной контекста - правила именования идентификатора Java
            return position == 0 && Character.isJavaIdentifierStart(character) ||
                    position > 0 && Character.isJavaIdentifierPart(character);
        }
    }

    /**
     *  В этом методе происходит сохранение информации об операнде - название аргумента или переменной контекста
     * @param isArgumentName является ли названием аргумента
     * @param isLeft находится ли слева от сравнения
     */
    private void endVariableNameReading(boolean isArgumentName, boolean isLeft) {
        OperandState operandState = isLeft ? state.leftOperandState :
                state.rightOperandState;
        operandState.resultValue = operandState.valueBuilder.toString();
        operandState.resultValueType = isArgumentName ? ValueType.GRAPHQL_ARGUMENT_NAME :
                ValueType.GRAPHQL_CONTEXT_FIELD_NAME;
        endOperandReading(isLeft);
    }

    /**
     * В этом методе происходит сохранение информации об операнде и переход из чтения операнда
     * @param isLeft находится ли слева от сравнения
     */
    private void endOperandReading(boolean isLeft) {
        /* если операнд - слева, переходим в метод ожидания оператора сравнения,
        если справа - добавляем операнд в выражение и переходим в метод ожидания закрывающей скобки или оператора комбинации*/
        if (!isLeft) {
            addParsedOperandComparison();
            waitCombiningOperatorOrRightParenthesis();
        } else {
            waitComparisonOperator();
        }
    }

    /**
     * В этом методе происходит сохранение значений оператора IN
     */
    private void endInReading() {
        InValuesState inValuesState = state.inValuesState;
        //выбираем оператор комбинирования в зависимости от типа оператора IN / NOT IN
        OperatorToken operator = inValuesState.negated ? AND : OR;
        //скобки нужны, если количество значений IN > 1
        boolean addParentheses = inValuesState.inValues.size() > 1;

        /* если нужно, добавляем скобки;
        добавляем сравнения с значениями IN и операторы комбинирования между ними */
        if (addParentheses) {
            state.result.addToken(LEFT_PARENTHESIS);
        }
        inValuesState.inValues.stream().findFirst().ifPresent(this::addInValueComparison);
        inValuesState.inValues.stream().skip(1).forEachOrdered(inValue -> {
            state.result.addToken(operator);
            addInValueComparison(inValue);
        });
        if (addParentheses) {
            state.result.addToken(RIGHT_PARENTHESIS);
        }
        //переходим в метод ожидания закрывающих скобок и операторов комбинации
        waitCombiningOperatorOrRightParenthesis();
    }

    /**
     * В этом методе происходит добавление сравнения значения IN и левого операнда в выражение
     * @param inValue значение IN
     */
    private void addInValueComparison(String inValue) {
        OperandState firstValue = state.leftOperandState;
        //создаем сравнение типа "равно" с левым операндом и значением IN, имеющим тип String
        ComparisonToken comparisonToken = ComparisonToken.builder()
                .firstValue(firstValue.resultValue, firstValue.resultValueType)
                .secondValue(inValue, ValueType.STRING)
                .comparisonType(ComparisonType.EQUALS)
                .build();
        //если оператор - NOT IN, добавляем оператор отрицания в выражение
        if (state.inValuesState.negated) {
            state.result.addToken(NOT);
        }
        //добавляем сравнение в выражение
        state.result.addToken(comparisonToken);
    }

    /**
     * В этом методе происходит парсинг и сохраненние типа и значения объекта
     * @param readingObjectState состояние левого или правого операнда
     */
    private void parseAndSaveObject(OperandState readingObjectState) {
        //преобразуем StringBuilder в строку, если значение пустое, выбрасываем исключение
        String stringValue = readingObjectState.valueBuilder.toString().trim();
        if (stringValue.isEmpty()) {
            throw new InvalidExpressionException("Object is empty", state.currentPosition,
                    state.expression);
        }

        //если значение - null, сохраняем тип null и нулевое значение
        if ("null".equals(stringValue)) {
            readingObjectState.resultValue = ComparisonToken.NullValue.INSTANCE;
            readingObjectState.resultValueType = ValueType.NULL;
            return;
        }
        //если тип - true или false, сохраняем тип boolean и соответствующее значение
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

        //если удается распарсить в один из типов java.time, сохраняем соответсвующий тип и значение
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
        Optional<LocalDate> localDate = tryParseDateTime(stringValue, LocalDate::from, DateTimeFormatter.ISO_LOCAL_DATE);
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

        //если удается распарсить число, сохраняем соответствующий тип и значение
        Optional<Number> numberOptional = tryParseNumber(stringValue);
        if (numberOptional.isPresent()) {
            Number number = numberOptional.get();
            readingObjectState.resultValue = number;
            readingObjectState.resultValueType = getNumberValueType(number);
            return;
        }

        //если не удалось распарсить ни в один из типов, выбрасываем исключение
        throw new InvalidExpressionException("Object can't be parsed", state.currentPosition, state.expression);
    }

    /**
     * Метод для получения даты/времени java.time из строки
     * @param value строка с датой/временем
     * @param temporalQuery temporalQuery необходимого типа java.time
     * @param formatter dateTimeFormatter для преобразования строки в необходимый тип
     * @param <T> тип java.time ({@link ZonedDateTime}, {@link LocalDateTime}, {@link LocalTime}, {@link LocalDate})
     * @return {@link Optional} с полученным значением даты/времени
     * или {@link Optional#empty()} в случае неудачного преобразования
     */
    private <T extends Temporal> Optional<T> tryParseDateTime(String value, TemporalQuery<T> temporalQuery,
                                                              DateTimeFormatter formatter) {
        try {
            return Optional.of(formatter.parse(value, temporalQuery));
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }

    /**
     * Метод для получения числа из строки
     * @param value строка с числом
     * @return {@link Optional} с полученным значением числа ({@link Long} или {@link Double})
     * или {@link Optional#empty()} в случае неудачного преобразования
     */
    private Optional<Number> tryParseNumber(String value) {
        ParsePosition position = new ParsePosition(0);
        Number number = NumberFormat.getInstance(Locale.US).parse(value, position);
        //если исходная строка не является числом, тогда индекс не соответствует длине исходной строки
        if (position.getIndex() != value.length()) {
            return Optional.empty();
        }
        return Optional.of(number);
    }

    /**
     * Метод для получения ValueType из объекта числа
     * @param number число
     * @return тип значения
     */
    private ValueType getNumberValueType(Number number) {
        if (number instanceof Long) {
            return ValueType.INTEGER;
        } else {
            return ValueType.REAL;
        }
    }

    /**
     * В этом методе происходит добавление сравнения в выражение из значений, сохраненных в состояниях операндов
     */
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

    /**
     * @return текущий символ
     * @throws InvalidExpressionException если символ отсутствует
     */
    private char getCurrentCharOrElseThrow() {
        return state.getCurrentChar().orElseThrow(() ->
                new InvalidExpressionException("Illegal expression", state.expression));
    }

    /**
     * @param symbol символ
     * @return является ли символ возможным началом идентификатора GraphQL
     */
    private static boolean isGraphQLNameStart(char symbol) {
        return (symbol >= 'A' && symbol <= 'Z') || (symbol >= 'a' && symbol <= 'z') || symbol == '_';
    }

    /**
     * @param symbol символ
     * @return является ли символ возможной частью идентификатора GraphQL
     */
    private static boolean isGraphQLNamePart(char symbol) {
        return isGraphQLNameStart(symbol) || (symbol >= '0' && symbol <= '9');
    }

    /**
     * Состояние текущего парсинга строки, обновляется при каждом вызове метода {@link #parse(String expression)}
     */
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

        /**
         * @return {@link Optional} с символом по текущему индексу, или {@link Optional#empty()}, если строка закончилась
         */
        Optional<Character> getCurrentChar() {
            if (currentPosition >= expressionLength) {
                return Optional.empty();
            }
            return Optional.of(expression.charAt(currentPosition));
        }
    }

    /**
     * Состояние чтения операнда
     */
    private static class OperandState {
        StringBuilder valueBuilder;
        Object resultValue;
        ValueType resultValueType;
        boolean escapeNextChar;

        public OperandState() {
            valueBuilder = new StringBuilder();
        }
    }

    /**
     * Состояние чтения оператора IN/NOT IN и его значений
     */
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
