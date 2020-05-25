package ru.liboskat.graphql.security.expression.transforming;

import ru.liboskat.graphql.security.storage.TokenExpression;

public interface RpnExpressionConverter {
    TokenExpression convertToRpn(TokenExpression tokenExpression);
}
