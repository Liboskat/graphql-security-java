package ru.liboskat.graphql.security.storage;

import static ru.liboskat.graphql.security.utils.StringUtils.isNullOrEmpty;

/**
 * Класс для хранения и передачи правил объекта применения в строковом виде
 */
public class StringExpressionRule {
    private final String readRule;
    private final String writeRule;
    private final String readWriteRule;

    private StringExpressionRule(String readRule, String writeRule, String readWriteRule) {
        this.readRule = readRule;
        this.writeRule = writeRule;
        this.readWriteRule = readWriteRule;
    }

    /**
     * @return правило чтения
     */
    public String getReadRule() {
        return readRule;
    }

    /**
     * @return правило записи
     */
    public String getWriteRule() {
        return writeRule;
    }

    /**
     * @return правило чтения/записи
     */
    public String getReadWriteRule() {
        return readWriteRule;
    }

    /**
     * @return {@link Builder} этого класса
     */
    public static StringExpressionRule.Builder newRule() {
        return new StringExpressionRule.Builder();
    }

    /**
     * Класс, используемый для создания нового {@link StringExpressionRule}
     */
    public static class Builder {
        private String readRule;
        private String writeRule;
        private String readWriteRule;

        /**
         * @see #readRule(String rule)
         */
        public Builder r(String rule) {
            return readRule(rule);
        }

        /**
         * @see #writeRule(String rule)
         */
        public Builder w(String rule) {
            return writeRule(rule);
        }

        /**
         * @see #readWriteRule(String rule)
         */
        public Builder rw(String rule) {
            return readWriteRule(rule);
        }

        /**
         * @see #readRule(String rule)
         */
        public Builder read(String rule) {
            return readRule(rule);
        }

        /**
         * @see #writeRule(String rule)
         */
        public Builder write(String rule) {
            return writeRule(rule);
        }

        /**
         * @see #readWriteRule(String rule)
         */
        public Builder readWrite(String rule) {
            return readWriteRule(rule);
        }

        /**
         * Сохраняет правило чтения в строковом виде
         *
         * @param rule правило чтения в строковом виде
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException, если правило - null или пустое
         */
        public Builder readRule(String rule) {
            throwExceptionIfNullOrEmpty(rule);
            this.readRule = rule;
            return this;
        }

        /**
         * Сохраняет правило записи в строковом виде
         *
         * @param rule правило записи в строковом виде
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException, если правило - null или пустое
         */
        public Builder writeRule(String rule) {
            throwExceptionIfNullOrEmpty(rule);
            this.writeRule = rule;
            return this;
        }

        /**
         * Сохраняет правило чтения/записи в строковом виде
         *
         * @param rule правило чтения/записи в строковом виде
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException, если правило - null или пустое
         */
        public Builder readWriteRule(String rule) {
            throwExceptionIfNullOrEmpty(rule);
            this.readWriteRule = rule;
            return this;
        }

        /**
         * Выбрасывает исключение, если правило - null или пустое
         *
         * @param rule строковое правило контроля доступа
         * @throws IllegalArgumentException, если правило - null или пустое
         */
        private void throwExceptionIfNullOrEmpty(String rule) {
            if (isNullOrEmpty(rule)) {
                throw new IllegalArgumentException("Rule can't be null or empty");
            }
        }

        /**
         * Создает новый {@link StringExpressionRule} с переданными строковыми правилами
         *
         * @return новый {@link StringExpressionRule} с переданными строковыми правилами
         * @throws IllegalArgumentException, если ни одно из правил не передано
         */
        public StringExpressionRule build() {
            if (isNullOrEmpty(readRule) && isNullOrEmpty(writeRule) && isNullOrEmpty(readWriteRule)) {
                throw new IllegalArgumentException("At least one of rules must be not empty");
            }
            return new StringExpressionRule(readRule, writeRule, readWriteRule);
        }
    }

    @Override
    public String toString() {
        return "StringExpressionRule{" +
                "readRule='" + readRule + '\'' +
                ", writeRule='" + writeRule + '\'' +
                ", readWriteRule='" + readWriteRule + '\'' +
                '}';
    }
}
