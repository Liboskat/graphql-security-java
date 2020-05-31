package ru.liboskat.graphql.security.storage;

import static ru.liboskat.graphql.security.utils.StringUtils.isNullOrEmpty;

/**
 * todo javadoc
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

    public static StringExpressionRule.Builder newRule() {
        return new StringExpressionRule.Builder();
    }

    public static class Builder {
        private String readRule;
        private String writeRule;
        private String readWriteRule;

        public Builder r(String rule) {
            return readRule(rule);
        }

        public Builder w(String rule) {
            return writeRule(rule);
        }

        public Builder rw(String rule) {
            return readWriteRule(rule);
        }

        public Builder read(String rule) {
            return readRule(rule);
        }

        public Builder write(String rule) {
            return writeRule(rule);
        }

        public Builder readWrite(String rule) {
            return readWriteRule(rule);
        }

        public Builder readRule(String rule) {
            throwExceptionIfNullOrEmpty(rule);
            this.readRule = rule;
            return this;
        }

        public Builder writeRule(String rule) {
            throwExceptionIfNullOrEmpty(rule);
            this.writeRule = rule;
            return this;
        }

        public Builder readWriteRule(String rule) {
            throwExceptionIfNullOrEmpty(rule);
            this.readWriteRule = rule;
            return this;
        }

        private void throwExceptionIfNullOrEmpty(String rule) {
            if (isNullOrEmpty(rule)) {
                throw new IllegalArgumentException("Rule can't be null or empty");
            }
        }

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
