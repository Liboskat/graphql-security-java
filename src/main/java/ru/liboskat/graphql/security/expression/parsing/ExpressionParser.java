package ru.liboskat.graphql.security.expression.parsing;

import ru.liboskat.graphql.security.storage.TokenExpression;

/**
 * Interface that is used to write classes that parse string expressions
 */
public interface ExpressionParser {
    /**
     * @param expression some string expression
     * @return parsed expression
     */
    TokenExpression parse(String expression);
}
