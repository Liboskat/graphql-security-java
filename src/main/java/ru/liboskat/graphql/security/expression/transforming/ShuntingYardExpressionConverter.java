package ru.liboskat.graphql.security.expression.transforming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liboskat.graphql.security.storage.token.ComparisonToken;
import ru.liboskat.graphql.security.storage.token.OperatorToken;
import ru.liboskat.graphql.security.storage.TokenExpression;

import java.util.LinkedList;

/**
 * An implementation of {@link RpnExpressionConverter} that is used to convert {@link TokenExpression} in infix notation
 * to Reverse Polish Notation using Shunting Yard algorithm
 */
public class ShuntingYardExpressionConverter implements RpnExpressionConverter {
    private static final Logger logger = LoggerFactory.getLogger(ShuntingYardExpressionConverter.class);

    /**
     * Converts {@link TokenExpression} in infix notation to Reverse Polish Notation using
     * Shunting Yard algorithm
     * @param tokenExpression {@link TokenExpression} in infix notation
     * @return {@link TokenExpression} in RPN
     * @throws IllegalArgumentException if number of right parens more than number of left
     */
    @Override
    public TokenExpression convertToRpn(TokenExpression tokenExpression) {
        logger.debug("Converting expression {} to RPN started", tokenExpression);
        TokenExpression rpnExpression = new TokenExpression();
        LinkedList<OperatorToken> operatorStack = new LinkedList<>();
        tokenExpression.getTokens().forEach(token -> {
            if (token instanceof ComparisonToken) {
                rpnExpression.addToken(token);
            } else if (token instanceof OperatorToken) {
                processOperator(rpnExpression, (OperatorToken) token, operatorStack);
            }
        });
        while (!operatorStack.isEmpty()) {
            rpnExpression.addToken(operatorStack.pop());
        }
        logger.debug("Converting expression {} to RPN ended. Converted expression {}", tokenExpression, rpnExpression);
        return rpnExpression;
    }

    private void processOperator(TokenExpression rpnExpression, OperatorToken operator,
                                 LinkedList<OperatorToken> operatorStack) {
        if (operator == OperatorToken.NOT) {
            operatorStack.push(operator);
        }
        if (operator == OperatorToken.AND || operator == OperatorToken.OR) {
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
        if (operator == OperatorToken.LEFT_PARENTHESIS) {
            operatorStack.push(operator);
        }
        if (operator == OperatorToken.RIGHT_PARENTHESIS) {
            boolean leftParenFound = false;
            while (operatorStack.peekFirst() != null && !leftParenFound) {
                if (operatorStack.peekFirst() == OperatorToken.LEFT_PARENTHESIS) {
                    leftParenFound = true;
                } else {
                    rpnExpression.addToken(operatorStack.pop());
                }
            }
            if (leftParenFound) {
                operatorStack.pop();
            } else {
                throw new IllegalArgumentException("Number of right parens more than number of left");
            }
        }
    }
}
