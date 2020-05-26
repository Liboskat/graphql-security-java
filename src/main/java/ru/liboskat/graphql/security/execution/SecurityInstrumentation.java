package ru.liboskat.graphql.security.execution;

import graphql.ExecutionResult;
import graphql.execution.AbortExecutionException;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.MergedField;
import graphql.execution.instrumentation.ExecutionStrategyInstrumentationContext;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
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
    public InstrumentationState createState() {
        return new SecurityInstrumentationState();
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecuteOperation(InstrumentationExecuteOperationParameters parameters) {
        SecurityInstrumentationState state = parameters.getInstrumentationState();
        ExecutionContext execContext = parameters.getExecutionContext();

        SecurityContext securityContext = retrieveSecurityContext(execContext.getContext());
        OperationType operationType;
        try {
            operationType = retrieveOperationType(execContext.getOperationDefinition());
        } catch (IllegalArgumentException e) {
            execContext.addError(new AuthException(e.getMessage()));
            state.hasErrors = true;
            throw new AbortExecutionException(execContext.getErrors());
        }

        try {
            accessRuleStorage.getSchemaRule()
                    .ifPresent(rule -> checkRule(operationType, rule, securityContext, null));
        } catch (AuthException e) {
            execContext.addError(e);
            state.hasErrors = true;
            throw new AbortExecutionException(execContext.getErrors());
        }

        state.securityContext = securityContext;
        state.operationType = operationType;
        return super.beginExecuteOperation(parameters);
    }

    @Override
    public ExecutionStrategyInstrumentationContext beginExecutionStrategy(InstrumentationExecutionStrategyParameters parameters) {
        ExecutionContext execContext = parameters.getExecutionContext();
        SecurityInstrumentationState state = parameters.getInstrumentationState();
        if (state.hasErrors) {
            throw new AbortExecutionException(execContext.getErrors());
        }
        try {
            checkAccess(parameters.getExecutionStrategyParameters(), state);
        } catch (AuthException e) {
            execContext.addError(e);
            state.hasErrors = true;
            throw new AbortExecutionException(execContext.getErrors());
        }
        return super.beginExecutionStrategy(parameters);
    }

    private void checkAccess(ExecutionStrategyParameters execParams,
                             SecurityInstrumentationState state) {
        OperationType operationType = state.operationType;
        SecurityContext securityContext = state.securityContext;

        GraphQLObjectType type = (GraphQLObjectType) execParams.getExecutionStepInfo().getUnwrappedNonNullType();
        String typeName = type.getName();
        if (state.isNotCheckedObject(typeName)) {
            accessRuleStorage.getObjectRule(typeName)
                    .ifPresent(rule -> checkRule(operationType, rule, securityContext, null));
            state.checkedObjects.add(typeName);
        }

        Set<GraphQLInput> inputsToCheck = new HashSet<>();
        execParams.getFields().getSubFields().values()
                .forEach(mergedField -> processField(inputsToCheck, type, operationType, mergedField, securityContext));
        inputsToCheck.forEach(input -> checkInput(input, operationType, securityContext, state));
    }

    private void processField(Set<GraphQLInput> inputsToCheck, GraphQLObjectType parentType, OperationType operationType,
                              MergedField mergedField, SecurityContext securityContext) {
        String fieldName = mergedField.getName();
        GraphQLFieldDefinition fieldDefinition = parentType.getFieldDefinition(fieldName);
        mergedField.getFields().forEach(field -> {
            Map<String, String> stringArguments = new HashMap<>();
            field.getArguments()
                    .forEach(arg -> processArgument(inputsToCheck, stringArguments, operationType, parentType.getName(),
                            fieldDefinition, arg, securityContext));
            accessRuleStorage.getFieldRule(parentType.getName(), fieldName)
                    .ifPresent(rule -> checkRule(operationType, rule, securityContext, stringArguments));
        });
    }

    private void processArgument(Set<GraphQLInput> inputsToCheck, Map<String, String> stringArguments,
                                 OperationType operationType, String parentTypeName,
                                 GraphQLFieldDefinition fieldDefinition, Argument argument,
                                 SecurityContext securityContext) {
        if (argument == null || argument.getName() == null || argument.getValue() == null) {
            return;
        }
        accessRuleStorage.getArgumentRule(parentTypeName, fieldDefinition.getName(), argument.getName())
                .ifPresent(rule -> checkRule(operationType, rule, securityContext, null));
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


    private void checkInput(GraphQLInput input, OperationType operationType, SecurityContext ctx,
                            SecurityInstrumentationState state) {
        if (input.getType() instanceof GraphQLInputObjectType && input.getValue() instanceof ObjectValue) {
            GraphQLInputObjectType type = (GraphQLInputObjectType) input.getType();
            String typeName = type.getName();
            ObjectValue value = (ObjectValue) input.getValue();
            if (state.isNotCheckedInput(typeName)) {
                accessRuleStorage.getInputObjectRule(typeName)
                        .ifPresent(rule -> checkRule(operationType, rule, ctx, null));
                state.checkedInputs.add(typeName);
            }
            value.getObjectFields().stream()
                    .filter(objectField -> objectField.getName() != null && objectField.getValue() != null)
                    .peek(objectField -> accessRuleStorage.getInputFieldRule(type.getName(), objectField.getName())
                            .ifPresent(rule -> checkRule(operationType, rule, ctx, null)))
                    .forEach(objectField -> {
                        GraphQLInputObjectField fieldDef = type.getField(objectField.getName());
                        GraphQLType fieldType = fieldDef != null ? fieldDef.getType() : null;
                        if (fieldType instanceof GraphQLNonNull) {
                            fieldType = ((GraphQLNonNull) fieldType).getWrappedType();
                        }
                        if (fieldType instanceof GraphQLNamedInputType) {
                            checkInput(new GraphQLInput(objectField.getValue(), (GraphQLNamedInputType) fieldType),
                                    operationType, ctx, state);
                        }
                    });
        }
    }

    private void checkRule(OperationType operationType, TokenExpressionRule rule, SecurityContext ctx,
                           Map<String, String> arguments) {
        TokenExpression expression;
        switch (operationType) {
            case READ:
                expression = rule.getReadRule();
                break;
            case WRITE:
                expression = rule.getWriteRule();
                break;
            default:
                throw new AuthException("Undefined operation");
        }
        try {
            if (!tokenExpressionSolver.solve(expression, ctx, arguments)) {
                throw new AuthException(rule.getTargetInfo());
            }
        } catch (IllegalArgumentException e) {
            throw new AuthException(rule.getTargetInfo());
        }
    }

    private OperationType retrieveOperationType(OperationDefinition operationDefinition) {
        if (operationDefinition == null || operationDefinition.getOperation() == null) {
            throw new IllegalArgumentException("Undefined operation " + operationDefinition);
        }
        if ("MUTATION".equals(operationDefinition.getOperation().name())) {
            return OperationType.WRITE;
        } else if ("QUERY".equals(operationDefinition.getOperation().name()) ||
                "SUBSCRIPTION".equals(operationDefinition.getOperation().name())) {
            return OperationType.READ;
        }
        throw new IllegalArgumentException("Undefined operation " + operationDefinition.getOperation().name());
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

    public enum OperationType {
        READ,
        WRITE
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

    private static class SecurityInstrumentationState implements InstrumentationState {
        private boolean hasErrors;
        private Set<String> checkedInputs;
        private Set<String> checkedObjects;
        private OperationType operationType;
        private SecurityContext securityContext;

        SecurityInstrumentationState() {
            this.checkedInputs = new HashSet<>();
            this.checkedObjects = new HashSet<>();
        }

        boolean isNotCheckedInput(String input) {
            return !checkedInputs.contains(input);
        }

        boolean isNotCheckedObject(String object) {
            return !checkedObjects.contains(object);
        }
    }
}
