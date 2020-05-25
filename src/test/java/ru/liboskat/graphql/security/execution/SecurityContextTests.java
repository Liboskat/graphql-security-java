package ru.liboskat.graphql.security.execution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityContextTests {
    @Test
    void addValue_ShouldBeAdded() {
        SecurityContext securityContext = SecurityContext.newSecurityContext()
                .field("key", 1)
                .build();
        assertEquals(securityContext.get("key"), 1);
    }

    @Test
    void addValue_keyIsNull_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> SecurityContext.newSecurityContext().field(null, null));
    }
}
