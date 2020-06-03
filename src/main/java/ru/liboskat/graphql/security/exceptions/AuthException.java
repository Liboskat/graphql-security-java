package ru.liboskat.graphql.security.exceptions;


import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import ru.liboskat.graphql.security.storage.ruletarget.RuleTargetInfo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Выбрасывается в случае остутствия доступа
 * Может быть добавлен в список ошибок запроса GraphQL
 */
public class AuthException extends RuntimeException implements GraphQLError {
    public static final String TARGET_ATTRIBUTE_NAME = "target";
    public static final String STATUS_CODE_ATTRIBUTE_NAME = "statusCode";
    public static final int STATUS_CODE_NUMBER = 403;

    private String ruleTargetInfo;

    public AuthException() {
        super();
    }

    public AuthException(String message) {
        super(message);
    }

    /**
     * Создает исключение с информацией об объекте, к которому был запрещен доступ
     *
     * @param ruleTargetInfo объект, к которому был запрещен доступ
     */
    public AuthException(RuleTargetInfo ruleTargetInfo) {
        super(String.format("Access denied to %s", ruleTargetInfo));
        this.ruleTargetInfo = ruleTargetInfo.toString();
    }

    @Override
    public List<SourceLocation> getLocations() {
        return Collections.emptyList();
    }

    @Override
    public ErrorClassification getErrorType() {
        return ErrorType.DataFetchingException;
    }

    @Override
    public Map<String, Object> getExtensions() {
        Map<String, Object> customAttributes = new LinkedHashMap<>();
        if (ruleTargetInfo != null) {
            customAttributes.put(TARGET_ATTRIBUTE_NAME, ruleTargetInfo);
        }
        customAttributes.put(STATUS_CODE_ATTRIBUTE_NAME, STATUS_CODE_NUMBER);
        return customAttributes;
    }
}
