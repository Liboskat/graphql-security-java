package ru.liboskat.graphql.security.storage;

import org.junit.jupiter.api.Test;
import ru.liboskat.graphql.security.storage.ruletarget.ObjectInfo;
import ru.liboskat.graphql.security.storage.token.OperatorToken;

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
                .targetInfo(ObjectInfo.newObjectInfo("object"))
                .build();
        assertEquals(ObjectInfo.newObjectInfo("object"), tokenExpressionRule.getTargetInfo());
    }
}
