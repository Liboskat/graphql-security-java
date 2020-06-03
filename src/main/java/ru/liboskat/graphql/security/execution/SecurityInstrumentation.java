package ru.liboskat.graphql.security.execution;

import graphql.ExecutionResult;
import graphql.execution.AbortExecutionException;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.MergedField;
import graphql.execution.instrumentation.*;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionStrategyParameters;
import graphql.language.*;
import graphql.schema.*;
import graphql.util.LogKit;
import org.slf4j.Logger;
import ru.liboskat.graphql.security.exceptions.AuthException;
import ru.liboskat.graphql.security.storage.AccessRuleStorage;
import ru.liboskat.graphql.security.storage.TokenExpression;
import ru.liboskat.graphql.security.storage.TokenExpressionRule;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Реализация интерфейса {@link Instrumentation}, позволяющая производить проверки контроля доступа
 */
public class SecurityInstrumentation extends SimpleInstrumentation {
    private static final Logger logNotSafe = LogKit.getNotPrivacySafeLogger(SecurityInstrumentation.class);

    private final AccessRuleStorage accessRuleStorage;
    private final TokenExpressionSolver tokenExpressionSolver;

    private SecurityInstrumentation(AccessRuleStorage accessRuleStorage) {
        this.accessRuleStorage = accessRuleStorage;
        this.tokenExpressionSolver = new TokenExpressionSolverImpl();
    }

    /**
     * @return новый {@link SecurityInstrumentationState}
     */
    @Override
    public InstrumentationState createState() {
        return new SecurityInstrumentationState();
    }

    /**
     * Выполняется перед каждым исполнением запроса, после парсинга и валидации.
     * Заполняет {@link SecurityInstrumentationState}, проверяет доступ к схеме
     *
     * @param parameters параметры выполнения запроса
     * @return результат работы родительского метода
     * @throws AbortExecutionException если доступ запрещен или выражение неверно
     */
    @Override
    public InstrumentationContext<ExecutionResult> beginExecuteOperation(InstrumentationExecuteOperationParameters parameters) {
        SecurityInstrumentationState state = parameters.getInstrumentationState();
        ExecutionContext execContext = parameters.getExecutionContext();

        logNotSafe.debug("Started checking operation {}", execContext.getOperationDefinition());

        //добавляем тип операции в state или останавливаем выполнение запроса
        addOperationTypeToStateOrElseAbortExecution(execContext, state);
        //добавляем контекст в state, если есть
        addSecurityContextToState(execContext.getContext(), state);
        //проверяем правило схемы
        checkSchemaRule(execContext, state);
        return super.beginExecuteOperation(parameters);
    }

    /**
     * Выполняется перед запросом к полям объекта. Проверяет доступ к объекту, запрашиваемым полям, аргументам,
     * переданным входным объектам, полям входных объектов
     *
     * @param parameters параметры выполнения запроса к полю
     * @return результат работы родительского метода
     * @throws AbortExecutionException если доступ запрещен
     */
    @Override
    public ExecutionStrategyInstrumentationContext beginExecutionStrategy(InstrumentationExecutionStrategyParameters parameters) {
        ExecutionContext execContext = parameters.getExecutionContext();
        SecurityInstrumentationState state = parameters.getInstrumentationState();

        //если уже есть ошибки, завершаем работу
        if (state.hasErrors) {
            throw new AbortExecutionException(execContext.getErrors());
        }
        //проверяем доступ на основе параметров и состояния
        try {
            checkAccess(parameters.getExecutionStrategyParameters(), state);
        } catch (AuthException e) {
            //если доступ запрещен, завершаем работу и сохраняем ошибку
            logNotSafe.warn("Access denied on {}", execContext.getOperationDefinition());
            execContext.addError(e);
            state.hasErrors = true;
            throw new AbortExecutionException(execContext.getErrors());
        }
        return super.beginExecutionStrategy(parameters);
    }

    /**
     * Возвращает тип операции, если типа нет, выбрасывает исключение завершения выполнения запроса
     *
     * @param executionContext контекст выполнения запроса
     * @param state            состояние
     * @throws AbortExecutionException если тип неизвестен
     */
    private void addOperationTypeToStateOrElseAbortExecution(ExecutionContext executionContext,
                                                             SecurityInstrumentationState state) {
        Optional<OperationType> operationTypeOptional = retrieveOperationType(executionContext.getOperationDefinition());
        if (!operationTypeOptional.isPresent()) {
            //если тип неизвестен, сохраняем информацию об ошибке, выбрасываем исключение
            logNotSafe.warn("Failed to retrieve operation type on {}", executionContext.getOperationDefinition());
            executionContext.addError(new AuthException("Failed to retrieve operation type"));
            state.hasErrors = true;
            throw new AbortExecutionException(executionContext.getErrors());
        }
        state.operationType = operationTypeOptional.get();
    }

    /**
     * Возвращает тип операции из {@link OperationDefinition}
     *
     * @param operationDefinition определение операции
     * @return {@link Optional} с типом операции или {@link Optional#empty()} если тип неизвестен
     */
    private Optional<OperationType> retrieveOperationType(OperationDefinition operationDefinition) {
        if (operationDefinition == null || operationDefinition.getOperation() == null) {
            return Optional.empty();
        }
        if ("MUTATION".equals(operationDefinition.getOperation().name())) {
            return Optional.of(OperationType.WRITE);
        } else if ("QUERY".equals(operationDefinition.getOperation().name()) ||
                "SUBSCRIPTION".equals(operationDefinition.getOperation().name())) {
            return Optional.of(OperationType.READ);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Добавляет найденный {@link SecurityContext} в {@link SecurityInstrumentationState}
     *
     * @param context объект контекста
     * @param state   состояние
     */
    private void addSecurityContextToState(Object context, SecurityInstrumentationState state) {
        if (context instanceof SecurityContext) {
            //если контекст сразу подходит, возвращаем контекст
            state.securityContext = (SecurityContext) context;
        } else if (context instanceof Map<?, ?>) {
            //если контекст - Map, ищем SecurityContext среди значений
            getFromMapContext((Map<?, ?>) context).ifPresent(state::setSecurityContext);
        } else if (context != null) {
            //если контекст - объект, ищем SecurityContext среди полей
            getFromObjectContext(context).ifPresent(state::setSecurityContext);
        }
    }

    /**
     * Ищет и возвращает найденный {@link SecurityContext} из {@link Map}
     *
     * @param context объект контекста с типом {@link Map}
     * @return {@link Optional} если {@link SecurityContext} найден, иначе {@link Optional#empty()}
     */
    private Optional<SecurityContext> getFromMapContext(Map<?, ?> context) {
        return context.values().stream()
                .filter(SecurityContext.class::isInstance)
                .findFirst()
                .map(SecurityContext.class::cast);
    }

    /**
     * Ищет и возвращает найденный {@link SecurityContext} из полей объекта контекста
     *
     * @param context объект контекста
     * @return {@link Optional} если {@link SecurityContext} найден, иначе {@link Optional#empty()}
     */
    private Optional<SecurityContext> getFromObjectContext(Object context) {
        for (Field field : context.getClass().getDeclaredFields()) {
            //если тип поля не соотвествует классу SecurityContext пропускаем итерацию
            if (!field.getType().equals(SecurityContext.class)) {
                continue;
            }
            //делаем возможным получение приватных полей
            field.setAccessible(true);
            SecurityContext securityContext = null;
            //пытаемся получить значение поля
            try {
                securityContext = (SecurityContext) field.get(context);
            } catch (IllegalAccessException e) {
                logNotSafe.warn("Failed access to context field");
            }
            //если securityContext получен из поля, возвращаем Optional с SecurityContext
            if (securityContext != null) {
                return Optional.of(securityContext);
            }
        }
        //если ничего не нашли, возвращаем пустой Optional
        return Optional.empty();
    }


    /**
     * Проверяет доступ к схеме
     *
     * @param executionContext контекст выполнения запроса
     * @param state            состояние
     * @throws AbortExecutionException если доступ запрещен
     */
    private void checkSchemaRule(ExecutionContext executionContext, SecurityInstrumentationState state) {
        try {
            //если правило есть, проверяем
            accessRuleStorage.getSchemaRule()
                    .ifPresent(rule -> checkRule(state.operationType, rule, state.securityContext, null));
        } catch (AuthException e) {
            //доступ запрещен -> сохраняем информацию, выбрасываем исключение
            logNotSafe.warn("Access denied on {}", executionContext.getOperationDefinition());
            executionContext.addError(e);
            state.hasErrors = true;
            throw new AbortExecutionException(executionContext.getErrors());
        }
    }

    /**
     * Проверяет доступ к объекту, запрашиваемым полям, аргументам, переданным входным объектам, полям входных объектов
     *
     * @param execParams параметры выполнения запроса к полю
     * @param state      состояние
     * @throws AuthException если доступ запрещен
     */
    private void checkAccess(ExecutionStrategyParameters execParams,
                             SecurityInstrumentationState state) {
        OperationType operationType = state.operationType;
        SecurityContext securityContext = state.securityContext;

        //получаем тип объекта, убирая обертку NonNull, и проверяем его
        GraphQLObjectType type = (GraphQLObjectType) execParams.getExecutionStepInfo().getUnwrappedNonNullType();
        checkObject(type, state);

        //проходим по запрашиваемым полям: сохраняем входные объекты для проверки, проверяем поля и аргументы
        Set<GraphQLInput> inputsToCheck = new HashSet<>();
        execParams.getFields().getSubFields().values()
                .forEach(mergedField -> processField(inputsToCheck, type, operationType, mergedField, securityContext));
        //проверяем переданные входные объекты
        inputsToCheck.forEach(input -> checkInput(input, operationType, securityContext, state));
    }

    /**
     * Проверяем доступ к объекту
     *
     * @param type  тип объекта
     * @param state состояние
     * @throws AuthException если доступ запрещен
     */
    private void checkObject(GraphQLObjectType type, SecurityInstrumentationState state) {
        String typeName = type.getName();
        //если объект еще не проверен, проверяем и добавляем в список проверенных
        if (state.isNotCheckedObject(typeName)) {
            accessRuleStorage.getObjectRule(typeName)
                    .ifPresent(rule -> checkRule(state.operationType, rule, state.securityContext, null));
            state.checkedObjects.add(typeName);
        }
    }

    /**
     * Проверяет доступ к полю, аргументам, заполняет список проверяемых входных объектов
     *
     * @param inputsToCheck   список проверяемых входных объектов
     * @param parentType      тип родительского объекта
     * @param operationType   тип операции
     * @param mergedField     поле
     * @param securityContext контекст безопасности
     * @throws AuthException если доступ запрещен
     */
    private void processField(Set<GraphQLInput> inputsToCheck, GraphQLObjectType parentType, OperationType operationType,
                              MergedField mergedField, SecurityContext securityContext) {
        String fieldName = mergedField.getName();
        GraphQLFieldDefinition fieldDefinition = parentType.getFieldDefinition(fieldName);
        //проходим по всем полям объекта с таким же названием (например из фрагмента)
        mergedField.getFields().forEach(field -> processSubMergedField(inputsToCheck,
                field, fieldDefinition, parentType.getName(), operationType, securityContext));
    }

    /**
     * Проходит по полю из MergedField
     *
     * @param inputsToCheck   список проверяемых входных объектов
     * @param field           описание поля с информацией из запроса
     * @param fieldDefinition определение поля
     * @param parentTypeName  название родительского типа
     * @param operationType   тип операции
     * @param securityContext контекст безопасности
     * @throws AuthException если доступ запрещен
     */
    private void processSubMergedField(Set<GraphQLInput> inputsToCheck,
                                       graphql.language.Field field, GraphQLFieldDefinition fieldDefinition,
                                       String parentTypeName, OperationType operationType,
                                       SecurityContext securityContext) {
        Map<String, String> stringArguments = new HashMap<>();
        //обходим все аргументы поля, проверяем аргумент, заполняем список входных объектов и значения аргументов
        field.getArguments().forEach(arg ->
                processArgument(inputsToCheck, stringArguments, operationType, parentTypeName,
                        fieldDefinition, arg, securityContext));
        //проверяем доступ к полю в соответствии с сохраненными значениями аргументов
        accessRuleStorage.getFieldRule(parentTypeName, fieldDefinition.getName())
                .ifPresent(rule -> checkRule(operationType, rule, securityContext, stringArguments));
    }

    /**
     * Проходит по аргументу, проверяет аргумент, заполняет список входных объектов и значения аргументов
     *
     * @param inputsToCheck   список проверяемых входных объектов
     * @param stringArguments {@link Map} название аргумента -> значение аргумента
     * @param operationType   тип операции
     * @param parentTypeName  название родительского типа
     * @param fieldDefinition определение поля
     * @param argument        аргумент из запроса
     * @param securityContext контекст безопасности
     * @throws AuthException если доступ запрещен
     */
    private void processArgument(Set<GraphQLInput> inputsToCheck, Map<String, String> stringArguments,
                                 OperationType operationType, String parentTypeName,
                                 GraphQLFieldDefinition fieldDefinition, Argument argument,
                                 SecurityContext securityContext) {
        //если аргумент пуст, останавливаем выполнение метода
        if (argument == null || argument.getName() == null || argument.getValue() == null) {
            return;
        }
        //проверяем доступ к аргументу
        accessRuleStorage.getArgumentRule(parentTypeName, fieldDefinition.getName(), argument.getName())
                .ifPresent(rule -> checkRule(operationType, rule, securityContext, null));

        //получаем тип аргумента
        GraphQLType argType = fieldDefinition.getArgument(argument.getName()).getType();
        //если обернут в NonNull, убираем обертку
        if (argType instanceof GraphQLNonNull) {
            argType = ((GraphQLNonNull) argType).getWrappedType();
        }
        Value<?> argValue = argument.getValue();
        //если тип объект - добавляем в список входных объекты для проверки
        if (argType instanceof GraphQLInputObjectType && argValue instanceof ObjectValue) {
            inputsToCheck.add(new GraphQLInput((ObjectValue) argValue, (GraphQLInputObjectType) argType));
        }
        //если значение аргумента - String, добавляем аргумент в Map - название аргумента -> значение аргумента
        if (argument.getValue() instanceof StringValue) {
            stringArguments.put(argument.getName(), ((StringValue) argument.getValue()).getValue());
        }
    }


    /**
     * Проверяет входной объект и его поля
     *
     * @param input         входной объект
     * @param operationType тип операции
     * @param context       контекст безопасности
     * @param state         состояние
     * @throws AuthException если доступ запрещен
     */
    private void checkInput(GraphQLInput input, OperationType operationType, SecurityContext context,
                            SecurityInstrumentationState state) {
        GraphQLInputObjectType type = input.type;

        //проверяем доступ к входному объекту
        checkInputTypeRule(operationType, context, state, type);

        //получаем все непустые поля входного объекта
        List<ObjectField> objectFields = input.value.getObjectFields().stream()
                .filter(this::notEmpty)
                .collect(Collectors.toList());

        //проверяем правила полей входного объекта
        objectFields.forEach(field -> checkInputFieldRule(operationType, context, type.getName(), field));

        //проверяем все типы полей, если они являются входными объектами
        objectFields.stream()
                //пытаемся получить входной объект из поля
                .map(field -> getGraphQLInputFromInputField(type.getField(field.getName()), field))
                //убираем неудавшиеся
                .filter(Optional::isPresent)
                .map(Optional::get)
                //проверяем входные объекты
                .forEach(inputObject -> checkInput(inputObject, operationType, context, state));
    }

    /**
     * @param objectField поле входного объекта
     * @return является ли поле пустым
     */
    private boolean notEmpty(ObjectField objectField) {
        return objectField.getName() != null && objectField.getValue() != null;
    }

    /**
     * Проверяет тип входного объекта, если он еще не проверен
     *
     * @param operationType тип операции
     * @param context       контекст безопасности
     * @param state         состояние
     * @param type          тип объекта
     * @throws AuthException если доступ запрещен
     */
    private void checkInputTypeRule(OperationType operationType, SecurityContext context,
                                    SecurityInstrumentationState state,
                                    GraphQLInputObjectType type) {
        String typeName = type.getName();
        //проверяем тип входного объекта, если он еще не проверен, и добавляем в список проверенных
        if (state.isNotCheckedInput(typeName)) {
            accessRuleStorage.getInputObjectRule(typeName)
                    .ifPresent(rule -> checkRule(operationType, rule, context, null));
            state.checkedInputs.add(typeName);
        }
    }

    /**
     * Проверяет поле входного объекта
     *
     * @param operationType  тип операции
     * @param context        контекст безопасности
     * @param parentTypeName название родительского типа
     * @param field          название поля
     * @throws AuthException если доступ запрещен
     */
    private void checkInputFieldRule(OperationType operationType, SecurityContext context,
                                     String parentTypeName, ObjectField field) {
        accessRuleStorage.getInputFieldRule(parentTypeName, field.getName())
                .ifPresent(rule -> checkRule(operationType, rule, context, null));
    }

    /**
     * Возвращает {@link Optional} с входным объектом - типом поля входного объекта
     *
     * @param fieldDef определение входного поля
     * @param field    значение входного поля
     * @return {@link Optional} с входным объектом, если тип поля - входной объект, иначе {@link Optional#empty()}
     */
    private Optional<GraphQLInput> getGraphQLInputFromInputField(GraphQLInputObjectField fieldDef, ObjectField field) {
        GraphQLType fieldType = fieldDef != null ? fieldDef.getType() : null;
        if (fieldType instanceof GraphQLNonNull) {
            fieldType = ((GraphQLNonNull) fieldType).getWrappedType();
        }
        Value<?> value = field.getValue();
        if (fieldType instanceof GraphQLInputObjectType && value instanceof ObjectValue) {
            return Optional.of(new GraphQLInput((ObjectValue) value, (GraphQLInputObjectType) fieldType));
        }
        return Optional.empty();
    }

    /**
     * Проверяет правило контроля доступа
     *
     * @param operationType тип операции
     * @param rule          выражение контроля доступа
     * @param ctx           контекст безопасности
     * @param arguments     аргументы поля, могут быть null
     * @throws AuthException если доступ запрещен
     */
    private void checkRule(OperationType operationType, TokenExpressionRule rule, SecurityContext ctx,
                           Map<String, String> arguments) {
        //определяем выражение по типу операции
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
        //если решение дало false или выражение некорректное, выбрасываем исключение с информацией об объекте
        try {
            if (!tokenExpressionSolver.solve(expression, ctx, arguments)) {
                throw new AuthException(rule.getTargetInfo());
            }
        } catch (IllegalArgumentException e) {
            throw new AuthException(rule.getTargetInfo());
        }
    }

    /**
     * @return {@link Builder} этого класса
     */
    public static SecurityInstrumentation.Builder newSecurityInstrumentation() {
        return new Builder();
    }

    /**
     * @return {@link Builder} этого класса с предустановленным {@link AccessRuleStorage}
     */
    public static SecurityInstrumentation.Builder newSecurityInstrumentation(AccessRuleStorage accessRuleStorage) {
        return new Builder().accessRuleStorage(accessRuleStorage);
    }

    /**
     * Класс, используемый для создания {@link SecurityInstrumentation}
     */
    public static class Builder {
        private AccessRuleStorage accessRuleStorage;

        private Builder() {
        }

        /**
         * Устанавливает {@link AccessRuleStorage}
         *
         * @param accessRuleStorage {@link AccessRuleStorage} с проверяемыми правилами
         * @return текущий {@link Builder}
         */
        public Builder accessRuleStorage(AccessRuleStorage accessRuleStorage) {
            this.accessRuleStorage = accessRuleStorage;
            return this;
        }

        /**
         * @return сконструированный {@link SecurityInstrumentation}
         * @throws IllegalArgumentException, если {@link AccessRuleStorage} не задан
         */
        public SecurityInstrumentation build() {
            if (accessRuleStorage == null) {
                throw new IllegalArgumentException("AccessRuleStorage can't be null");
            }
            return new SecurityInstrumentation(accessRuleStorage);
        }
    }

    /**
     * Тип операции
     */
    public enum OperationType {
        READ,
        WRITE
    }

    /**
     * Обертка для опреления и значения входного объекта
     */
    private static class GraphQLInput {
        private final ObjectValue value;
        private final GraphQLInputObjectType type;
        private final String typeName;

        GraphQLInput(ObjectValue value, GraphQLInputObjectType type) {
            this.value = value;
            this.type = type;
            this.typeName = type != null ? type.getName() : null;
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

    /**
     * Класс - реализация {@link InstrumentationState},
     * хранящая информацию о текущем выполнении {@link SecurityInstrumentation}.
     * Содержит информацию о наличии ошибок, проверенных входных и выходных объектах,
     * тип операции и контекст безопасности {@link SecurityContext}
     */
    private static class SecurityInstrumentationState implements InstrumentationState {
        private boolean hasErrors;
        private final Set<String> checkedInputs;
        private final Set<String> checkedObjects;
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

        public void setSecurityContext(SecurityContext securityContext) {
            this.securityContext = securityContext;
        }
    }
}
