package ru.liboskat.graphql.security.storage;

import org.junit.jupiter.api.Test;
import ru.liboskat.graphql.security.storage.token.OperatorToken;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class TokenExpressionTests {
    @Test
    void addToken_shouldBeAdded() {
        TokenExpression tokenExpression = new TokenExpression();
        tokenExpression.addToken(OperatorToken.AND);
        assertEquals(OperatorToken.AND, tokenExpression.getTokens().get(0));
    }

    @Test
    void addAllTokens_shouldBeAdded() {
        TokenExpression tokenExpression = new TokenExpression();
        tokenExpression.addAllTokens(Arrays.asList(OperatorToken.AND, OperatorToken.OR));
        assertIterableEquals(Arrays.asList(OperatorToken.AND, OperatorToken.OR), tokenExpression.getTokens());
    }

    @Test
    void isEmpty_emptyExpression_shouldBeTrue() {
        assertTrue(new TokenExpression().isEmpty());
    }

    @Test
    void isEmpty_notEmptyExpression_shouldBeFalse() {
        TokenExpression tokenExpression = new TokenExpression();
        tokenExpression.addToken(OperatorToken.AND);
        assertFalse(tokenExpression.isEmpty());
    }

    @Test
    void equalExpressions_shouldBeEqual() {
        TokenExpression first = new TokenExpression();
        first.addAllTokens(Arrays.asList(OperatorToken.AND, OperatorToken.OR));
        TokenExpression second = new TokenExpression();
        second.addAllTokens(Arrays.asList(OperatorToken.AND, OperatorToken.OR));
        assertEquals(first, second);
    }

    @Test
    void unequalExpressions_shouldNotBeEqual() {
        TokenExpression first = new TokenExpression();
        first.addAllTokens(Arrays.asList(OperatorToken.AND, OperatorToken.OR));
        TokenExpression second = new TokenExpression();
        second.addAllTokens(Arrays.asList(OperatorToken.OR, OperatorToken.AND));
        assertNotEquals(first, second);
    }
}
