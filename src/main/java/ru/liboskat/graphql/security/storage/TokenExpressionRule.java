package ru.liboskat.graphql.security.storage;

import ru.liboskat.graphql.security.storage.ruletarget.RuleTargetInfo;

public class TokenExpressionRule {
    private final TokenExpression readRule;
    private final TokenExpression writeRule;
    private final RuleTargetInfo targetInfo;

    private TokenExpressionRule(TokenExpression readRule, TokenExpression writeRule, RuleTargetInfo targetInfo) {
        this.readRule = readRule;
        this.writeRule = writeRule;
        this.targetInfo = targetInfo;
    }

    public TokenExpression getReadRule() {
        return readRule;
    }

    public TokenExpression getWriteRule() {
        return writeRule;
    }

    public RuleTargetInfo getTargetInfo() {
        return targetInfo;
    }

    static TokenExpressionRule.Builder builder() {
        return new Builder();
    }

    static class Builder {
        private TokenExpression readRule;
        private TokenExpression writeRule;
        private RuleTargetInfo targetInfo;

        private Builder() {}

        Builder readRule(TokenExpression readRule) {
            this.readRule = readRule;
            return this;
        }

        Builder writeRule(TokenExpression writeRule) {
            this.writeRule = writeRule;
            return this;
        }

        Builder targetInfo(RuleTargetInfo targetInfo) {
            this.targetInfo = targetInfo;
            return this;
        }

        TokenExpressionRule build() {
            return new TokenExpressionRule(readRule, writeRule, targetInfo);
        }
    }

    @Override
    public String toString() {
        return "TokenExpressionRule{" +
                "readRule=" + readRule +
                ", writeRule=" + writeRule +
                '}';
    }
}
