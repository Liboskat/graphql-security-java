package ru.liboskat.graphql.security.expression.parsing;

import ru.liboskat.graphql.security.storage.TokenExpression;

public interface ExpressionParser {
    TokenExpression parse(String expression);
}
