package ru.liboskat.graphql.security.storage.ruletarget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FieldInfoTests {
    @Test
    void equalFieldInfo_shouldBeEqual() {
        assertEquals(FieldInfo.newFieldInfo("object", "field"),
                FieldInfo.newFieldInfo("object", "field"));
    }

    @Test
    void unequalFieldInfo_shouldBeNotEqual() {
        assertNotEquals(FieldInfo.newFieldInfo("object", "field"),
                FieldInfo.newFieldInfo("object", "not same"));
    }
}
