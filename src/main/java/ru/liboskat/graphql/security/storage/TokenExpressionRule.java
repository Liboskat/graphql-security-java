package ru.liboskat.graphql.security.storage;

/**
 * Class used to store and transfer rules in {@link TokenExpression} form with information about rule target
 */
public class TokenExpressionRule {
    private final TokenExpression readRule;
    private final TokenExpression writeRule;
    private final AccessRuleStorage.RuleTargetInfo targetInfo;

    private TokenExpressionRule(TokenExpression readRule, TokenExpression writeRule, AccessRuleStorage.RuleTargetInfo targetInfo) {
        this.readRule = readRule;
        this.writeRule = writeRule;
        this.targetInfo = targetInfo;
    }

    /**
     * @return combined read rules and read-write rules
     */
    public TokenExpression getReadRule() {
        return readRule;
    }

    /**
     * @return combined write rules and read-write rules
     */
    public TokenExpression getWriteRule() {
        return writeRule;
    }

    /**
     * @return information about target of rule
     */
    public AccessRuleStorage.RuleTargetInfo getTargetInfo() {
        return targetInfo;
    }

    static TokenExpressionRule.Builder builder() {
        return new Builder();
    }

    static class Builder {
        private TokenExpression readRule;
        private TokenExpression writeRule;
        private AccessRuleStorage.RuleTargetInfo targetInfo;

        private Builder() {}

        Builder readRule(TokenExpression readRule) {
            this.readRule = readRule;
            return this;
        }

        Builder writeRule(TokenExpression writeRule) {
            this.writeRule = writeRule;
            return this;
        }

        Builder targetInfo(AccessRuleStorage.RuleTargetInfo targetInfo) {
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
