package ru.liboskat.graphql.security.storage.ruletarget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class InputFieldInfoTests {
    @Test
    void equalInputFieldInfo_shouldBeEqual() {
        assertEquals(InputFieldInfo.newInputFieldInfo("object", "field"),
                InputFieldInfo.newInputFieldInfo("object", "field"));
    }

    @Test
    void unequalInputFieldInfo_shouldBeNotEqual() {
        assertNotEquals(InputFieldInfo.newInputFieldInfo("object", "field"),
                InputFieldInfo.newInputFieldInfo("object", "not same"));
    }
}
