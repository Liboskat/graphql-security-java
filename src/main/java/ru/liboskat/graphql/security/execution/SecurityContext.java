package ru.liboskat.graphql.security.execution;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to store context that may be used in access control checks
 */
public class SecurityContext {
    private final Map<String, Object> contextFields;

    private SecurityContext(Map<String, Object> contextFields) {
        this.contextFields = contextFields;
    }

    /**
     * @param key key of field
     * @return value of field
     */
    public Object get(String key) {
        return contextFields.get(key);
    }

    /**
     * @return {@link Builder} of this class
     */
    public static SecurityContext.Builder newSecurityContext() {
        return new Builder();
    }

    /**
     * Class used to construct {@link SecurityContext}
     */
    public static class Builder {
        private final Map<String, Object> contextFields;

        private Builder() {
            this.contextFields = new HashMap<>();
        }

        /**
         * Adds field to context
         * @param key key of field
         * @param value value of field
         * @return this builder
         */
        public Builder field(String key, Object value) {
            if (key == null) {
                throw new IllegalArgumentException("Key can't be null");
            }
            contextFields.put(key, value);
            return this;
        }

        /**
         * @return built {@link SecurityContext}
         */
        public SecurityContext build() {
            return new SecurityContext(contextFields);
        }
    }
}
