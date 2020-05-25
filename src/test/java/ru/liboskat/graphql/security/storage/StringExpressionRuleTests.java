package ru.liboskat.graphql.security.storage;

import org.junit.jupiter.api.Test;
import ru.liboskat.graphql.security.storage.StringExpressionRule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StringExpressionRuleTests {
    @Test
    void addReadWriteRule_shouldBeAdded() {
        StringExpressionRule rule = StringExpressionRule.newRule()
                .readWriteRule("rw")
                .build();
        assertEquals("rw", rule.getReadWriteRule());
    }

    @Test
    void addWriteRule_shouldBeAdded() {
        StringExpressionRule rule = StringExpressionRule.newRule()
                .writeRule("w")
                .build();
        assertEquals("w", rule.getWriteRule());
    }

    @Test
    void addReadRule_shouldBeAdded() {
        StringExpressionRule rule = StringExpressionRule.newRule()
                .readRule("r")
                .build();
        assertEquals("r", rule.getReadRule());
    }

    @Test
    void addReadWriteRule_null_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> StringExpressionRule.newRule().readWriteRule(null));
    }

    @Test
    void addReadRule_null_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> StringExpressionRule.newRule().readRule(null));
    }

    @Test
    void addWriteRule_null_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> StringExpressionRule.newRule().writeRule(null));
    }

    @Test
    void build_withoutRules_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> StringExpressionRule.newRule().build());
    }
}
