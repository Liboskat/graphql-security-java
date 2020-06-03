package ru.liboskat.graphql.security.exceptions;

/**
 * Выбрасывается если выражение контроля доступа является неверным
 */
public class InvalidExpressionException extends RuntimeException {
    private static final String MESSAGE_FORMAT = "%s in \"%s\"";
    private static final String POSITION_MESSAGE_FORMAT = "%s, position %d in \"%s\"";
    private static final String ILLEGAL_SYMBOL_FORMAT = "Illegal symbol '%c' on position %d, expected %s in \"%s\"";

    /**
     * Создает исключение с некорректным выражением и неким сообщением
     *
     * @param message    сообщение
     * @param expression некорректное выражение
     */
    public InvalidExpressionException(String message, String expression) {
        super(String.format(MESSAGE_FORMAT, message, expression));
    }

    /**
     * Создает исключение с некорректным выражением, неким сообщением и позицией ошибки
     *
     * @param position   позиция ошибки
     * @param message    сообщение
     * @param expression некорректное выражение
     */
    public InvalidExpressionException(String message, int position, String expression) {
        super(String.format(POSITION_MESSAGE_FORMAT, message, position, expression));
    }

    /**
     * Создает исключение с некорректным выражением, позицией ошибки, ожидаемым и полученным символом
     *
     * @param symbol     некорректный полученный символ
     * @param position   позиция ошибки
     * @param expected   сообщение с информацией об ожидаемых символах
     * @param expression некорректное выражение
     */
    public InvalidExpressionException(char symbol, int position, String expected, String expression) {
        super(String.format(ILLEGAL_SYMBOL_FORMAT, symbol, position, expected, expression));
    }
}
