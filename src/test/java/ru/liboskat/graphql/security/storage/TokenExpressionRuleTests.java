package ru.liboskat.graphql.security.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenExpressionRuleTests {
    @Test
    void addWriteRule_shouldBeAdded() {
        TokenExpression tokenExpression = new TokenExpression();
        tokenExpression.addToken(OperatorToken.AND);
        TokenExpressionRule tokenExpressionRule = TokenExpressionRule.builder()
                .writeRule(tokenExpression)
                .build();
        assertEquals(tokenExpression, tokenExpressionRule.getWriteRule());
    }

    @Test
    void addReadRule_shouldBeAdded() {
        TokenExpression tokenExpression = new TokenExpression();
        tokenExpression.addToken(OperatorToken.AND);
        TokenExpressionRule tokenExpressionRule = TokenExpressionRule.builder()
                .readRule(tokenExpression)
                .build();
        assertEquals(tokenExpression, tokenExpressionRule.getReadRule());
    }

    @Test
    void addTargetInfo_shouldBeAdded() {
        TokenExpression tokenExpression = new TokenExpression();
        tokenExpression.addToken(OperatorToken.AND);
        TokenExpressionRule tokenExpressionRule = TokenExpressionRule.builder()
                .readRule(tokenExpression)
                .targetInfo(new AccessRuleStorage.ObjectInfo("object"))
                .build();
        assertEquals(new AccessRuleStorage.ObjectInfo("object"), tokenExpressionRule.getTargetInfo());
    }
}
