package ru.liboskat.graphql.security.storage;

/**
 * Class that is used to store and transfer rules in String form
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

    public String getReadRule() {
        return readRule;
    }

    public String getWriteRule() {
        return writeRule;
    }

    public String getReadWriteRule() {
        return readWriteRule;
    }

    /**
     * @return {@link Builder} that is used to construct new instance
     */
    public static StringExpressionRule.Builder newRule() {
        return new StringExpressionRule.Builder();
    }

    /**
     * Class is used to construct new {@link StringExpressionRule}
     */
    public static class Builder {
        private String readRule;
        private String writeRule;
        private String readWriteRule;

        /**
         * @param rule read rule
         * @return this builder
         * @throws IllegalArgumentException if rule is null or empty
         */
        public Builder r(String rule) {
            return readRule(rule);
        }

        /**
         * @param rule write rule
         * @return this builder
         * @throws IllegalArgumentException if rule is null or empty
         */
        public Builder w(String rule) {
            return writeRule(rule);
        }

        /**
         * @param rule read and write rule
         * @return this builder
         * @throws IllegalArgumentException if rule is null or empty
         */
        public Builder rw(String rule) {
            return readWriteRule(rule);
        }

        /**
         * @param rule read rule
         * @return this builder
         * @throws IllegalArgumentException if rule is null or empty
         */
        public Builder read(String rule) {
            return readRule(rule);
        }

        /**
         * @param rule write rule
         * @return this builder
         * @throws IllegalArgumentException if rule is null or empty
         */
        public Builder write(String rule) {
            return writeRule(rule);
        }

        /**
         * @param rule read and write rule
         * @return this builder
         * @throws IllegalArgumentException if rule is null or empty
         */
        public Builder readWrite(String rule) {
            return readWriteRule(rule);
        }

        /**
         * @param rule read rule
         * @return this builder
         * @throws IllegalArgumentException if rule is null or empty
         */
        public Builder readRule(String rule) {
            throwExceptionIfNullOrEmpty(rule);
            this.readRule = rule;
            return this;
        }

        /**
         * @param rule write rule
         * @return this builder
         * @throws IllegalArgumentException if rule is null or empty
         */
        public Builder writeRule(String rule) {
            throwExceptionIfNullOrEmpty(rule);
            this.writeRule = rule;
            return this;
        }

        /**
         * @param rule read and write rule
         * @return this builder
         * @throws IllegalArgumentException if rule is null or empty
         */
        public Builder readWriteRule(String rule) {
            throwExceptionIfNullOrEmpty(rule);
            this.readWriteRule = rule;
            return this;
        }

        private void throwExceptionIfNullOrEmpty(String rule) {
            if (rule == null || rule.isEmpty()) {
                throw new IllegalArgumentException("Rule can't be null or empty");
            }
        }

        /**
         * @return {@link StringExpressionRule} instance
         * @throws IllegalArgumentException if read rule and write rule and read-write rule is null or empty
         */
        public StringExpressionRule build() {
            if ((readRule == null || readRule.isEmpty()) && (writeRule == null || writeRule.isEmpty()) &&
                    (readWriteRule == null || readWriteRule.isEmpty())) {
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
