package ru.liboskat.graphql.security.expression.transforming;

import ru.liboskat.graphql.security.storage.ComparisonToken;
import ru.liboskat.graphql.security.storage.OperatorToken;
import ru.liboskat.graphql.security.storage.TokenExpression;

import java.util.LinkedList;

public class ShuntingYardExpressionConverter implements RpnExpressionConverter {
    @Override
    public TokenExpression convertToRpn(TokenExpression tokenExpression) {
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
                    topOfOperatorStack != OperatorToken.LEFT_PAREN) {
                rpnExpression.addToken(operatorStack.pop());
            }
            operatorStack.push(operator);
        }
        if (operator == OperatorToken.LEFT_PAREN) {
            operatorStack.push(operator);
        }
        if (operator == OperatorToken.RIGHT_PAREN) {
            boolean leftParenFound = false;
            while (operatorStack.peekFirst() != null && !leftParenFound) {
                if (operatorStack.peekFirst() == OperatorToken.LEFT_PAREN) {
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
