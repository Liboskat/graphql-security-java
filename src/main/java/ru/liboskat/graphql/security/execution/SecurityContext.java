package ru.liboskat.graphql.security.execution;

import java.util.HashMap;
import java.util.Map;

import static ru.liboskat.graphql.security.utils.StringUtils.isNullOrEmpty;

/**
 * Используется для хранения контекста, используемого при проверке выражений контроля доступа
 */
public class SecurityContext {
    private final Map<String, Object> contextFields;

    private SecurityContext(Map<String, Object> contextFields) {
        this.contextFields = contextFields;
    }

    public Object get(String key) {
        return contextFields.get(key);
    }

    public static SecurityContext.Builder newSecurityContext() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, Object> contextFields;

        private Builder() {
            this.contextFields = new HashMap<>();
        }

        public Builder field(String key, Object value) {
            if (isNullOrEmpty(key)) {
                throw new IllegalArgumentException("Key can't be null or empty");
            }
            contextFields.put(key, value);
            return this;
        }

        public SecurityContext build() {
            return new SecurityContext(contextFields);
        }
    }
}
