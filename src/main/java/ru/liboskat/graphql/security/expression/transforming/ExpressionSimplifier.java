package ru.liboskat.graphql.security.expression.transforming;

import ru.liboskat.graphql.security.storage.TokenExpression;

public interface ExpressionSimplifier {
    TokenExpression simplify(TokenExpression tokenExpression);
}
