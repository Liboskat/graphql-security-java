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

    /**
     * Возвращает значение переменной контекста по имени, или null если нет переменной с таким именем
     *
     * @param key имя переменной контекста
     * @return значение переменной контекста, или null если нет переменной с таким именем
     */
    public Object get(String key) {
        return contextFields.get(key);
    }

    /**
     * @return возвращает {@link Builder} для этого класса
     */
    public static SecurityContext.Builder newSecurityContext() {
        return new Builder();
    }

    /**
     * Используется для построения класса {@link SecurityContext}
     */
    public static class Builder {
        private final Map<String, Object> contextFields;

        private Builder() {
            this.contextFields = new HashMap<>();
        }

        /**
         * Метод для передачи переменной в контекст
         *
         * @param key   название переменной
         * @param value значение переменной
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException, если ключ пустой или null
         */
        public Builder field(String key, Object value) {
            if (isNullOrEmpty(key)) {
                throw new IllegalArgumentException("Key can't be null or empty");
            }
            contextFields.put(key, value);
            return this;
        }

        /**
         * @return {@link SecurityContext} с переданными переменными
         */
        public SecurityContext build() {
            return new SecurityContext(contextFields);
        }
    }
}
