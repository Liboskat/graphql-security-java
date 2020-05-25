package ru.liboskat.graphql.security.exceptions;


import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import ru.liboskat.graphql.security.storage.AccessRuleStorage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AuthException extends RuntimeException implements GraphQLError {
    private String ruleTargetInfo;

    public AuthException() {
        super();
    }

    public AuthException(String message) {
        super(message);
    }

    public AuthException(AccessRuleStorage.RuleTargetInfo ruleTargetInfo) {
        super(String.format("Access denied to %s", ruleTargetInfo));
        this.ruleTargetInfo = ruleTargetInfo.toString();
    }

    @Override
    public List<SourceLocation> getLocations() {
        return null;
    }

    @Override
    public ErrorClassification getErrorType() {
        return ErrorType.DataFetchingException;
    }

    @Override
    public Map<String, Object> getExtensions() {
        Map<String, Object> customAttributes = new LinkedHashMap<>();
        if (ruleTargetInfo != null) {
            customAttributes.put("target", ruleTargetInfo);
        }
        customAttributes.put("status_code", 403);
        return customAttributes;
    }
}
