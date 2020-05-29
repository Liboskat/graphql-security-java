package ru.liboskat.graphql.security.storage.ruletarget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class InputObjectInfoTests {
    @Test
    void equalInputObjectInfo_shouldBeEqual() {
        assertEquals(InputObjectInfo.newInputObjectInfo("object"), InputObjectInfo.newInputObjectInfo("object"));
    }

    @Test
    void unequalInputObjectInfo_shouldBeNotEqual() {
        assertNotEquals(InputObjectInfo.newInputObjectInfo("object"), InputObjectInfo.newInputObjectInfo("not same"));
    }
}
