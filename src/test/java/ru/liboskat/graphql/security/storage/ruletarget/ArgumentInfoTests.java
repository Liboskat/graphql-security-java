package ru.liboskat.graphql.security.storage.ruletarget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ArgumentInfoTests {
    @Test
    void equalArgumentInfo_shouldBeEqual() {
        assertEquals(ArgumentInfo.newArgumentInfo("object", "field", "arg"),
                ArgumentInfo.newArgumentInfo("object", "field", "arg"));
    }

    @Test
    void unequalArgumentInfo_shouldBeNotEqual() {
        assertNotEquals(ArgumentInfo.newArgumentInfo("object", "field", "arg"),
                ArgumentInfo.newArgumentInfo("object", "field", "not same"));
    }
}
