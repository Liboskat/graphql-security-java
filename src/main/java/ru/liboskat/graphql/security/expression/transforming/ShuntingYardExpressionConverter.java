package ru.liboskat.graphql.security.expression.transforming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liboskat.graphql.security.storage.token.ComparisonToken;
import ru.liboskat.graphql.security.storage.token.OperatorToken;
import ru.liboskat.graphql.security.storage.TokenExpression;

import java.util.LinkedList;

/**
 * Реализация {@link RpnExpressionConverter}, используемая для конвертирования {@link TokenExpression} в инфиксной записи
 * в обратную польскую, используя алгоритм сортировочной станции
 */
public class ShuntingYardExpressionConverter implements RpnExpressionConverter {
    private static final Logger logger = LoggerFactory.getLogger(ShuntingYardExpressionConverter.class);

    /**
     * Конвертирует выражение {@link TokenExpression} в инфиксной записи в обратную польскую,
     * используя алгоритм сортировочной станции
     * @param tokenExpression {@link TokenExpression} в инфиксной записи
     * @return {@link TokenExpression} в обратной польской записи
     * @throws IllegalArgumentException если число закрывающих скобок больше числа открывающих
     */
    @Override
    public TokenExpression convertToRpn(TokenExpression tokenExpression) {
        logger.debug("Converting expression {} to RPN started", tokenExpression);

        TokenExpression rpnExpression = new TokenExpression();
        LinkedList<OperatorToken> operatorStack = new LinkedList<>();
        tokenExpression.getTokens().forEach(token -> {
            if (token instanceof ComparisonToken) {
                //если токен - сравнение, добавляем токен в результат
                rpnExpression.addToken(token);
            } else if (token instanceof OperatorToken) {
                //если токен - оператор, обрабатываем оператор
                processOperator(rpnExpression, (OperatorToken) token, operatorStack);
            }
        });
        // добавляем все оставшиеся операторы из стека операторов в результат
        while (!operatorStack.isEmpty()) {
            rpnExpression.addToken(operatorStack.pop());
        }
        logger.debug("Converting expression {} to RPN ended. Converted expression {}", tokenExpression, rpnExpression);
        return rpnExpression;
    }

    /**
     * Обрабатывает оператор из исходного выражения
     * @param rpnExpression результат
     * @param operator оператор
     * @param operatorStack стек операторов
     * @throws IllegalArgumentException, если число закрывающих скобок больше чем число открывающих
     */
    private void processOperator(TokenExpression rpnExpression, OperatorToken operator,
                                 LinkedList<OperatorToken> operatorStack) {
        //если оператор - ! или (, добавляем его в стек операторов
        if (operator == OperatorToken.NOT | operator == OperatorToken.LEFT_PARENTHESIS) {
            operatorStack.push(operator);
        }
        if (operator == OperatorToken.AND || operator == OperatorToken.OR) {
            /* если оператор - & или |, пока не найден оператор наверху стека с меньшим приоритетом чем у текущего,
            перекладываем оператор сверху стека в результат
            Добавляем текущий оператор в стек */
            OperatorToken topOfOperatorStack;
            while (!operatorStack.isEmpty() &&
                    (topOfOperatorStack = operatorStack.peekFirst()) != null &&
                    (topOfOperatorStack == OperatorToken.NOT ||
                            topOfOperatorStack.getPrecedence() >= operator.getPrecedence()) &&
                    topOfOperatorStack != OperatorToken.LEFT_PARENTHESIS) {
                rpnExpression.addToken(operatorStack.pop());
            }
            operatorStack.push(operator);
        }
        if (operator == OperatorToken.RIGHT_PARENTHESIS) {
            //пока не найдена открывающая скобка, перекладываем все операторы из стека в результат
            boolean leftParenFound = false;
            while (operatorStack.peekFirst() != null && !leftParenFound) {
                if (operatorStack.peekFirst() == OperatorToken.LEFT_PARENTHESIS) {
                    leftParenFound = true;
                } else {
                    rpnExpression.addToken(operatorStack.pop());
                }
            }
            /* если нашли, удаляем скобку, иначе выбрасываем исключение,
            так как не найдена открывающая скобка для закрывающей */
            if (leftParenFound) {
                operatorStack.pop();
            } else {
                throw new IllegalArgumentException("Number of right parentheses more than number of left");
            }
        }
    }
}
