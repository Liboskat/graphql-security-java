package ru.liboskat.graphql.security.execution;

import graphql.execution.AbortExecutionException;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.MergedField;
import graphql.execution.instrumentation.ExecutionStrategyInstrumentationContext;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionStrategyParameters;
import graphql.language.*;
import graphql.schema.*;
import ru.liboskat.graphql.security.exceptions.AuthException;
import ru.liboskat.graphql.security.storage.AccessRuleStorage;
import ru.liboskat.graphql.security.storage.TokenExpression;
import ru.liboskat.graphql.security.storage.TokenExpressionRule;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;

public class SecurityInstrumentation extends SimpleInstrumentation {
    private final AccessRuleStorage accessRuleStorage;
    private final TokenExpressionSolver tokenExpressionSolver;

    private SecurityInstrumentation(AccessRuleStorage accessRuleStorage) {
        this.accessRuleStorage = accessRuleStorage;
        this.tokenExpressionSolver = new TokenExpressionSolverImpl();
    }

    @Override
    public ExecutionStrategyInstrumentationContext beginExecutionStrategy(InstrumentationExecutionStrategyParameters parameters) {
        ExecutionContext execContext = parameters.getExecutionContext();
        try {
            checkAccess(execContext, parameters.getExecutionStrategyParameters());
        } catch (AuthException e) {
            execContext.addError(e);
            throw new AbortExecutionException(execContext.getErrors());
        }
        return super.beginExecutionStrategy(parameters);
    }

    private void checkAccess(ExecutionContext execContext, ExecutionStrategyParameters execParams) {
        String operationName = retrieveOperationName(execContext.getOperationDefinition());

        SecurityContext securityContext = retrieveSecurityContext(execContext.getContext());

        accessRuleStorage.getSchemaRule()
                .ifPresent(rule -> checkRule(operationName, rule, securityContext, null));

        GraphQLObjectType type = (GraphQLObjectType) execParams.getExecutionStepInfo().getUnwrappedNonNullType();
        String typeName = type.getName();
        accessRuleStorage.getObjectRule(typeName)
                .ifPresent(rule -> checkRule(operationName, rule, securityContext, null));

        Set<GraphQLInput> inputsToCheck = new HashSet<>();
        execParams.getFields().getSubFields().values()
                .forEach(mergedField -> processField(inputsToCheck, type, operationName, mergedField, securityContext));
        inputsToCheck.forEach(input -> checkInput(input, operationName, securityContext));
    }

    private void processField(Set<GraphQLInput> inputsToCheck, GraphQLObjectType parentType, String operationName,
                              MergedField mergedField, SecurityContext securityContext) {
        String fieldName = mergedField.getName();
        GraphQLFieldDefinition fieldDefinition = parentType.getFieldDefinition(fieldName);
        mergedField.getFields().forEach(field -> {
            Map<String, String> stringArguments = new HashMap<>();
            field.getArguments()
                    .forEach(arg -> processArgument(inputsToCheck, stringArguments, operationName, parentType.getName(),
                            fieldDefinition, arg, securityContext));
            accessRuleStorage.getFieldRule(parentType.getName(), fieldName)
                    .ifPresent(rule -> checkRule(operationName, rule, securityContext, stringArguments));
        });
    }

    private void processArgument(Set<GraphQLInput> inputsToCheck, Map<String, String> stringArguments,
                                 String operationName, String parentTypeName, GraphQLFieldDefinition fieldDefinition,
                                 Argument argument, SecurityContext securityContext) {
        if (argument == null || argument.getName() == null || argument.getValue() == null) {
            return;
        }
        accessRuleStorage.getArgumentRule(parentTypeName, fieldDefinition.getName(), argument.getName())
                .ifPresent(rule -> checkRule(operationName, rule, securityContext, null));
        GraphQLArgument argDef = fieldDefinition.getArgument(argument.getName());
        GraphQLType argType = argDef != null ? argDef.getType() : null;
        if (argType instanceof GraphQLNonNull) {
            argType = ((GraphQLNonNull) argType).getWrappedType();
        }
        if (argType instanceof GraphQLNamedInputType) {
            inputsToCheck.add(new GraphQLInput(argument.getValue(), (GraphQLNamedInputType) argDef.getType()));
        }
        if (argument.getValue() instanceof StringValue) {
            stringArguments.put(argument.getName(), ((StringValue) argument.getValue()).getValue());
        }
    }


    private void checkInput(GraphQLInput input, String operationName, SecurityContext ctx) {
        if (input.getType() instanceof GraphQLInputObjectType && input.getValue() instanceof ObjectValue) {
            GraphQLInputObjectType type = (GraphQLInputObjectType) input.getType();
            ObjectValue value = (ObjectValue) input.getValue();
            accessRuleStorage.getInputObjectRule(type.getName())
                    .ifPresent(rule -> checkRule(operationName, rule, ctx, null));
            value.getObjectFields().stream()
                    .filter(objectField -> objectField.getName() != null && objectField.getValue() != null)
                    .peek(objectField -> accessRuleStorage.getInputFieldRule(type.getName(), objectField.getName())
                            .ifPresent(rule -> checkRule(operationName, rule, ctx, null)))
                    .forEach(objectField -> {
                        GraphQLInputObjectField fieldDef = type.getField(objectField.getName());
                        GraphQLType fieldType = fieldDef != null ? fieldDef.getType() : null;
                        if (fieldType instanceof GraphQLNonNull) {
                            fieldType = ((GraphQLNonNull) fieldType).getWrappedType();
                        }
                        if (fieldType instanceof GraphQLNamedInputType) {
                            checkInput(new GraphQLInput(objectField.getValue(), (GraphQLNamedInputType) fieldType),
                                    operationName, ctx);
                        }
                    });
        }
    }

    private void checkRule(String operationName, TokenExpressionRule rule, SecurityContext ctx,
                           Map<String, String> arguments) {
        boolean isRead = true;
        if ("MUTATION".equals(operationName)) {
            isRead = false;
        }
        TokenExpression expression = isRead ? rule.getReadRule() : rule.getWriteRule();
        try {
            if (!tokenExpressionSolver.solve(expression, ctx, arguments)) {
                throw new AuthException(rule.getTargetInfo());
            }
        } catch (IllegalArgumentException e) {
            throw new AuthException(rule.getTargetInfo());
        }
    }

    private String retrieveOperationName(OperationDefinition operationDefinition) {
        if (operationDefinition == null) {
            return null;
        }
        if (operationDefinition.getOperation() == null) {
            return null;
        }
        return operationDefinition.getOperation().name();
    }

    private SecurityContext retrieveSecurityContext(Object context) {
        if (context instanceof SecurityContext) {
            return (SecurityContext) context;
        } else if (context instanceof Map) {
            for (Object object : ((Map) context).entrySet()) {
                if (object instanceof Entry && ((Entry) object).getValue() instanceof SecurityContext) {
                    return (SecurityContext) ((Entry) object).getValue();
                }
            }
        } else if (context != null) {
            for (Field field : context.getClass().getDeclaredFields()) {
                if (SecurityContext.class.equals(field.getType())) {
                    field.setAccessible(true);
                    SecurityContext securityContext = null;
                    try {
                        securityContext = (SecurityContext) field.get(context);
                    } catch (IllegalAccessException e) {
                        //toDo log
                    }
                    if (securityContext != null) {
                        return securityContext;
                    }
                }
            }
        }
        return null;
    }

    public static SecurityInstrumentation.Builder newSecurityInstrumentation() {
        return new Builder();
    }

    public static SecurityInstrumentation.Builder newSecurityInstrumentation(AccessRuleStorage accessRuleStorage) {
        return new Builder().accessRuleStorage(accessRuleStorage);
    }


    public static class Builder {
        private AccessRuleStorage accessRuleStorage;

        private Builder() {
        }

        public Builder accessRuleStorage(AccessRuleStorage accessRuleStorage) {
            this.accessRuleStorage = accessRuleStorage;
            return this;
        }

        public SecurityInstrumentation build() {
            if (accessRuleStorage == null) {
                throw new IllegalArgumentException("AccessRuleStorage can't be null");
            }
            return new SecurityInstrumentation(accessRuleStorage);
        }
    }

    private static class GraphQLInput {
        private final Value value;
        private final GraphQLNamedInputType type;
        private final String typeName;

        GraphQLInput(Value value, GraphQLNamedInputType type) {
            this.value = value;
            this.type = type;
            this.typeName = type != null ? type.getName() : null;
        }

        Value getValue() {
            return value;
        }

        GraphQLNamedInputType getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GraphQLInput that = (GraphQLInput) o;

            return Objects.equals(typeName, that.typeName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeName);
        }
    }
}
