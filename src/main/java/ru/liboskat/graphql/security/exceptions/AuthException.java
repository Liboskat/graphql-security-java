package ru.liboskat.graphql.security.exceptions;


import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import ru.liboskat.graphql.security.storage.AccessRuleStorage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thrown if access to resource is denied.
 * Can be put to the list of GraphQL execution errors.
 */
public class AuthException extends RuntimeException implements GraphQLError {
    private String ruleTargetInfo;

    /**
     * Creates exception without information
     */
    public AuthException() {
        super();
    }


    /**
     * Creates exception with some message
     * @param message error message
     */
    public AuthException(String message) {
        super(message);
    }

    /**
     * Creates exception with information about target
     * @param ruleTargetInfo information about access check target
     */
    public AuthException(AccessRuleStorage.RuleTargetInfo ruleTargetInfo) {
        super(String.format("Access denied to %s", ruleTargetInfo));
        this.ruleTargetInfo = ruleTargetInfo.toString();
    }

    /**
     * This method is not implemented
     * @return null
     */
    @Override
    public List<SourceLocation> getLocations() {
        return null;
    }

    /**
     * @return {@link ErrorType} that is used by GraphQL Java
     */
    @Override
    public ErrorClassification getErrorType() {
        return ErrorType.DataFetchingException;
    }

    /**
     * @return extensions that have information about target and status code
     */
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
