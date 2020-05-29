package ru.liboskat.graphql.security.storage.ruletarget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ObjectInfoTests {
    @Test
    void equalObjectInfo_shouldBeEqual() {
        assertEquals(ObjectInfo.newObjectInfo("object"), ObjectInfo.newObjectInfo("object"));
    }

    @Test
    void unequalObjectInfo_shouldBeNotEqual() {
        assertNotEquals(ObjectInfo.newObjectInfo("object"), ObjectInfo.newObjectInfo("not same"));
    }
}
