package ru.liboskat.graphql.security.storage;

import ru.liboskat.graphql.security.storage.ruletarget.RuleTargetInfo;

/**
 * Класс для хранения и передачи правил объекта применения в объектном виде
 */
public class TokenExpressionRule {
    private final TokenExpression readRule;
    private final TokenExpression writeRule;
    private final RuleTargetInfo targetInfo;

    private TokenExpressionRule(TokenExpression readRule, TokenExpression writeRule, RuleTargetInfo targetInfo) {
        this.readRule = readRule;
        this.writeRule = writeRule;
        this.targetInfo = targetInfo;
    }

    /**
     * @return правило чтения
     */
    public TokenExpression getReadRule() {
        return readRule;
    }

    /**
     * @return правило записи
     */
    public TokenExpression getWriteRule() {
        return writeRule;
    }

    /**
     * @return класс с информацией об объекте применения
     */
    public RuleTargetInfo getTargetInfo() {
        return targetInfo;
    }

    /**
     * @return {@link Builder} для этого класса
     */
    static TokenExpressionRule.Builder builder() {
        return new Builder();
    }

    /**
     * Класс, используемый для создания нового {@link TokenExpressionRule}
     */
    static class Builder {
        private TokenExpression readRule;
        private TokenExpression writeRule;
        private RuleTargetInfo targetInfo;

        private Builder() {
        }

        /**
         * Сохраняет правило чтения в объектном виде
         *
         * @param readRule правило чтения в объектном виде
         * @return текущий {@link Builder}
         */
        Builder readRule(TokenExpression readRule) {
            this.readRule = readRule;
            return this;
        }

        /**
         * Сохраняет правило записи в объектном виде
         *
         * @param writeRule правило записи в объектном виде
         * @return текущий {@link Builder}
         */
        Builder writeRule(TokenExpression writeRule) {
            this.writeRule = writeRule;
            return this;
        }

        /**
         * Сохраняет информацию об объекте применения
         *
         * @param targetInfo объект применения правил контроля доступа
         * @return текущий {@link Builder}
         */
        Builder targetInfo(RuleTargetInfo targetInfo) {
            this.targetInfo = targetInfo;
            return this;
        }

        /**
         * Создает новый {@link TokenExpressionRule} с переданными правилами и объектом применения
         *
         * @return новый {@link StringExpressionRule} с переданными правилами и объектом применения
         */
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
