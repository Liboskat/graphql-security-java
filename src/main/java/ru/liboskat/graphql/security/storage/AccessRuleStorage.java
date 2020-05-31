package ru.liboskat.graphql.security.storage;

import graphql.Scalars;
import graphql.introspection.Introspection;
import graphql.language.*;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liboskat.graphql.security.exceptions.InvalidAuthDirectiveException;
import ru.liboskat.graphql.security.expression.parsing.ExpressionParser;
import ru.liboskat.graphql.security.expression.parsing.SimpleExpressionParser;
import ru.liboskat.graphql.security.expression.transforming.*;
import ru.liboskat.graphql.security.storage.ruletarget.*;

import java.util.*;

import static ru.liboskat.graphql.security.utils.StringUtils.isNullOrEmpty;

/**
 * Класс для хранения выражений контроля доступа по объектам применения
 */
public class AccessRuleStorage {
    private static final Logger logger = LoggerFactory.getLogger(AccessRuleStorage.class);

    public static final String AUTH_DIRECTIVE_NAME = "auth";

    private final TokenExpressionRule schemaRule;
    private final Map<ObjectInfo, TokenExpressionRule> objectRules;
    private final Map<FieldInfo, TokenExpressionRule> fieldRules;
    private final Map<ArgumentInfo, TokenExpressionRule> argumentRules;
    private final Map<InputObjectInfo, TokenExpressionRule> inputObjectRules;
    private final Map<InputFieldInfo, TokenExpressionRule> inputFieldRules;

    /**
     * Конструктор для AccessRuleStorage без значения правила схемы
     */
    private AccessRuleStorage(Map<ObjectInfo, TokenExpressionRule> objectRules,
                              Map<FieldInfo, TokenExpressionRule> fieldRules,
                              Map<ArgumentInfo, TokenExpressionRule> argumentRules,
                              Map<InputObjectInfo, TokenExpressionRule> inputObjectRules,
                              Map<InputFieldInfo, TokenExpressionRule> inputFieldRules) {
        this(null, objectRules, fieldRules, argumentRules, inputObjectRules, inputFieldRules);
    }

    private AccessRuleStorage(TokenExpressionRule schemaRule, Map<ObjectInfo, TokenExpressionRule> objectRules,
                              Map<FieldInfo, TokenExpressionRule> fieldRules,
                              Map<ArgumentInfo, TokenExpressionRule> argumentRules,
                              Map<InputObjectInfo, TokenExpressionRule> inputObjectRules,
                              Map<InputFieldInfo, TokenExpressionRule> inputFieldRules) {
        this.schemaRule = schemaRule;
        this.objectRules = objectRules;
        this.fieldRules = fieldRules;
        this.argumentRules = argumentRules;
        this.inputObjectRules = inputObjectRules;
        this.inputFieldRules = inputFieldRules;
    }

    /**
     * @return новый {@link Builder} необходимый для построения класса {@link AccessRuleStorage}
     */
    public static AccessRuleStorage.Builder newAccessRuleStorage() {
        return new Builder();
    }

    /**
     * @return {@link Optional} с выражением для схемы, если выражения нет - {@link Optional#empty()}
     */
    public Optional<TokenExpressionRule> getSchemaRule() {
        return Optional.ofNullable(schemaRule);
    }

    /**
     * @param objectName название объекта
     * @return {@link Optional} с выражением для объекта, если выражения нет - {@link Optional#empty()}
     */
    public Optional<TokenExpressionRule> getObjectRule(String objectName) {
        return Optional.ofNullable(objectRules.get(ObjectInfo.newObjectInfo(objectName)));
    }

    /**
     * @param objectName название объекта
     * @param fieldName название поля объекта
     * @return {@link Optional} с выражением для поля объекта, если выражения нет - {@link Optional#empty()}
     */
    public Optional<TokenExpressionRule> getFieldRule(String objectName, String fieldName) {
        return Optional.ofNullable(fieldRules.get(FieldInfo.newFieldInfo(objectName, fieldName)));
    }

    /**
     * @param objectName название объекта
     * @param fieldName название поля объекта
     * @param argumentName название аргумента поля
     * @return {@link Optional} с выражением для аргумента поля, если выражения нет - {@link Optional#empty()}
     */
    public Optional<TokenExpressionRule> getArgumentRule(String objectName, String fieldName, String argumentName) {
        return Optional.ofNullable(argumentRules.get(ArgumentInfo.newArgumentInfo(objectName, fieldName, argumentName)));
    }

    /**
     * @param inputObjectName название входного объекта
     * @return {@link Optional} с выражением для входного объекта, если выражения нет - {@link Optional#empty()}
     */
    public Optional<TokenExpressionRule> getInputObjectRule(String inputObjectName) {
        return Optional.ofNullable(inputObjectRules.get(InputObjectInfo.newInputObjectInfo(inputObjectName)));
    }

    /**
     * @param inputObjectName название входного объекта
     * @param inputFieldName название поля входного объекта
     * @return {@link Optional} с выражением для поля входного объекта, если выражения нет - {@link Optional#empty()}
     */
    public Optional<TokenExpressionRule> getInputFieldRule(String inputObjectName, String inputFieldName) {
        return Optional.ofNullable(inputFieldRules.get(InputFieldInfo.newInputFieldInfo(inputObjectName, inputFieldName)));
    }

    /**
     * Класс, используемый для конструирования нового {@link AccessRuleStorage}
     */
    public static class Builder {
        /**
         * названия аргументов директивы @auth
         */
        private static final List<String> DIRECTIVE_INPUT_VALUE_NAMES = Arrays.asList("rw", "r", "w");
        /**
         * названия локаций директивы @auth
         */
        private static final List<String> DIRECTIVE_LOCATIONS = Arrays.asList(
                Introspection.DirectiveLocation.SCHEMA.toString(),
                Introspection.DirectiveLocation.OBJECT.toString(),
                Introspection.DirectiveLocation.FIELD_DEFINITION.toString(),
                Introspection.DirectiveLocation.ARGUMENT_DEFINITION.toString(),
                Introspection.DirectiveLocation.INPUT_OBJECT.toString(),
                Introspection.DirectiveLocation.INPUT_FIELD_DEFINITION.toString()
        );
        /**
         * название типа аргументов директивы @auth
         */
        private static final String DIRECTIVE_INPUT_TYPE_NAME = Scalars.GraphQLString.getName();

        private final ExpressionParser expressionParser;
        private final RpnExpressionConverter rpnExpressionConverter;
        private final ExpressionSimplifier expressionSimplifier;
        private final TokenExpressionCombiner tokenExpressionCombiner;
        private final List<StringExpressionRule> schemaRules;
        private final Map<ObjectInfo, List<StringExpressionRule>> objectRules;
        private final Map<FieldInfo, List<StringExpressionRule>> fieldRules;
        private final Map<ArgumentInfo, List<StringExpressionRule>> argumentRules;
        private final Map<InputObjectInfo, List<StringExpressionRule>> inputObjectRules;
        private final Map<InputFieldInfo, List<StringExpressionRule>> inputFieldRules;
        boolean hasDirective;

        private Builder() {
            this.expressionParser = new SimpleExpressionParser();
            this.rpnExpressionConverter = new ShuntingYardExpressionConverter();
            this.expressionSimplifier = new QuineMcCluskeyExpressionSimplifier();
            this.tokenExpressionCombiner = new TokenExpressionConjunctCombiner();
            this.hasDirective = false;
            this.schemaRules = new ArrayList<>();
            this.objectRules = new HashMap<>();
            this.fieldRules = new HashMap<>();
            this.argumentRules = new HashMap<>();
            this.inputObjectRules = new HashMap<>();
            this.inputFieldRules = new HashMap<>();
        }

        /**
         * Добавляет правило схемы
         *
         * @param rule выражение в строковом виде
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException если выражение null или пустое
         */
        public Builder schemaRule(StringExpressionRule rule) {
            checkNotNull(rule);
            schemaRules.add(rule);
            return this;
        }

        /**
         * Добавляет строковое выражение для поля объекта
         *
         * @param rule       выражение в строковом виде
         * @param objectName название объекта
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException если выражение null или пустое
         */
        public Builder objectRule(StringExpressionRule rule, String objectName) {
            checkNotNull(rule);
            addRuleToMap(objectRules, ObjectInfo.newObjectInfo(objectName), rule);
            return this;
        }

        /**
         * Добавляет строковое выражение для поля объекта
         *
         * @param rule      выражение в строковом виде
         * @param typeName  название объекта
         * @param fieldName название поля
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException если выражение null или пустое
         */
        public Builder fieldRule(StringExpressionRule rule, String typeName, String fieldName) {
            checkNotNull(rule);
            addRuleToMap(fieldRules, FieldInfo.newFieldInfo(typeName, fieldName), rule);
            return this;
        }

        /**
         * Добавляет строковое выражение для аргумента поля
         *
         * @param rule         выражение в строковом виде
         * @param typeName     название объекта
         * @param fieldName    название поля
         * @param argumentName название аргумента
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException если выражение null или пустое
         */
        public Builder argumentRule(StringExpressionRule rule, String typeName, String fieldName, String argumentName) {
            checkNotNull(rule);
            addRuleToMap(argumentRules, ArgumentInfo.newArgumentInfo(typeName, fieldName, argumentName), rule);
            return this;
        }

        /**
         * Добавляет строковое выражение для входного объекта
         *
         * @param rule            выражение в строковом виде
         * @param inputObjectName название входного объекта
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException если выражение null или пустое
         */
        public Builder inputObjectRule(StringExpressionRule rule, String inputObjectName) {
            checkNotNull(rule);
            addRuleToMap(inputObjectRules, InputObjectInfo.newInputObjectInfo(inputObjectName), rule);
            return this;
        }

        /**
         * Добавляет строковое выражение для поля входного объекта
         *
         * @param rule          выражение в строковом виде
         * @param inputTypeName название входного объекта
         * @param fieldName     название поля
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException если выражение null или пустое
         */
        public Builder inputFieldRule(StringExpressionRule rule, String inputTypeName, String fieldName) {
            checkNotNull(rule);
            addRuleToMap(inputFieldRules, InputFieldInfo.newInputFieldInfo(inputTypeName, fieldName), rule);
            return this;
        }

        /**
         * Выбрасывает исключение, если выражение - null
         *
         * @param rule строковое выражение
         */
        private void checkNotNull(StringExpressionRule rule) {
            if (rule == null) {
                throw new IllegalArgumentException("Rule can't be null or empty");
            }
        }

        /**
         * Добавляет все правила из {@link TypeDefinitionRegistry}
         *
         * @param registry {@link TypeDefinitionRegistry} реестр типов GraphQL схемы
         * @return этот {@link Builder}
         * @throws InvalidAuthDirectiveException если имеющаяся директива @auth неверна
         */
        public Builder fromTypeDefinitionRegistry(TypeDefinitionRegistry registry) {
            //если директива @auth уже есть, проверяем ее корректность и помечаем, что директива уже имеется
            registry.getDirectiveDefinition(AUTH_DIRECTIVE_NAME).ifPresent(authDirective -> {
                checkAuthDirectiveDefinition(authDirective);
                hasDirective = true;
            });
            //добавляем правило для схемы
            registry.schemaDefinition().ifPresent(this::setRuleFromSchemaDefinition);
            //добавляем правила для типов
            registry.types().values().forEach(this::setRulesFromType);
            //если нет директивы @auth, создаем новую
            if (!hasDirective) {
                registry.add(buildAuthDirectiveDefinition());
            }
            return this;
        }

        /**
         * Трансформирует выражения в объектный вид и возвращает новый {@link AccessRuleStorage}
         *
         * @return новый {@link AccessRuleStorage}
         */
        public AccessRuleStorage build() {
            logger.debug("AccessRuleStorage building started");

            //преобразуем строковые выражения в объектный вид
            Map<ObjectInfo, TokenExpressionRule> objectRules = transformRuleMap(this.objectRules);
            Map<FieldInfo, TokenExpressionRule> fieldRules = transformRuleMap(this.fieldRules);
            Map<ArgumentInfo, TokenExpressionRule> argumentRules = transformRuleMap(this.argumentRules);
            Map<InputObjectInfo, TokenExpressionRule> inputObjectRules = transformRuleMap(this.inputObjectRules);
            Map<InputFieldInfo, TokenExpressionRule> inputFieldRules = transformRuleMap(this.inputFieldRules);
            Optional<TokenExpressionRule> schemaRuleOptional = transform(schemaRules, SchemaInfo.newSchemaInfo());

            //используется необходимый конструктор в зависимости от наличия директивы схемы
            AccessRuleStorage accessRuleStorage = schemaRuleOptional
                    .map(tokenExpressionRule ->
                            new AccessRuleStorage(tokenExpressionRule, objectRules, fieldRules, argumentRules,
                                    inputObjectRules, inputFieldRules))
                    .orElseGet(() ->
                            new AccessRuleStorage(objectRules, fieldRules, argumentRules,
                                    inputObjectRules, inputFieldRules));

            logger.debug("AccessRuleStorage building ended");
            return accessRuleStorage;
        }

        /**
         * Проверяет корректность директивы @auth в схеме
         *
         * @param directiveDefinition определение схемы
         * @throws InvalidAuthDirectiveException если директива неправильна
         */
        private void checkAuthDirectiveDefinition(DirectiveDefinition directiveDefinition) {
            List<InputValueDefinition> inputValueDefinitions = directiveDefinition.getInputValueDefinitions();
            List<DirectiveLocation> directiveLocations = directiveDefinition.getDirectiveLocations();

            boolean nameIsCorrect = AUTH_DIRECTIVE_NAME.equals(directiveDefinition.getName());

            /* количество аргументов должно быть не больше чем в идеальной директиве,
            все типы должны соответствовать типу аргументов идеальной директивы,
            название аргумента должно быть одним из возможных названий аргументов директивы */
            boolean inputValuesIsCorrect = DIRECTIVE_INPUT_VALUE_NAMES.size() >= inputValueDefinitions.size() &&
                    inputValueDefinitions.stream()
                            .map(InputValueDefinition::getType)
                            .allMatch(type -> type instanceof TypeName &&
                                    DIRECTIVE_INPUT_TYPE_NAME.equals(((TypeName) type).getName())) &&
                    inputValueDefinitions.stream()
                            .map(InputValueDefinition::getName)
                            .allMatch(DIRECTIVE_INPUT_VALUE_NAMES::contains);

            /* количество локаций должно быть не больше чем в идеальной директиве,
            локация должна быть одной из возможных локаций директивы */
            boolean locationsIsCorrect = DIRECTIVE_LOCATIONS.size() >= directiveLocations.size() &&
                    directiveLocations.stream()
                            .map(DirectiveLocation::getName)
                            .allMatch(DIRECTIVE_LOCATIONS::contains);


            boolean correct = nameIsCorrect && inputValuesIsCorrect && locationsIsCorrect;
            if (!correct) {
                throw new InvalidAuthDirectiveException();
            }
        }

        /**
         * Создает правильное определение директивы @auth
         *
         * @return определение директивы @auth
         */
        private DirectiveDefinition buildAuthDirectiveDefinition() {
            DirectiveDefinition.Builder directiveBuilder = DirectiveDefinition.newDirectiveDefinition();
            directiveBuilder.name(AUTH_DIRECTIVE_NAME);
            //добавляем аргументы директивы
            DIRECTIVE_INPUT_VALUE_NAMES.forEach(inputValueName ->
                    directiveBuilder.inputValueDefinition(InputValueDefinition.newInputValueDefinition()
                            .name(inputValueName)
                            .type(TypeName.newTypeName(DIRECTIVE_INPUT_TYPE_NAME).build())
                            .build()
                    ));
            //добавляем местоположения директивы
            DIRECTIVE_LOCATIONS.forEach(locationName ->
                    directiveBuilder.directiveLocation(DirectiveLocation.newDirectiveLocation()
                            .name(locationName)
                            .build()));
            return directiveBuilder.build();
        }

        /**
         * Добавляет строковое выражение для схемы
         *
         * @param schema определение схемы
         */
        private void setRuleFromSchemaDefinition(SchemaDefinition schema) {
            findAuthDirectiveAndGetRule(schema.getDirectives()).ifPresent(schemaRules::add);
        }

        /**
         * Добавляет строковые выражения для типов
         *
         * @param type определение типов
         */
        private void setRulesFromType(TypeDefinition<?> type) {
            if (type instanceof InputObjectTypeDefinition) {
                //если тип - входной объект
                setRulesFromInputObject((InputObjectTypeDefinition) type);
            } else if (type instanceof ObjectTypeDefinition) {
                //если тип - выходной объект
                setRulesFromObject((ObjectTypeDefinition) type);
            }
        }

        /**
         * Добавляет строковые выражения для входных объектов
         *
         * @param inputObject определение входного объекта
         */
        private void setRulesFromInputObject(InputObjectTypeDefinition inputObject) {
            //если есть выражение из директивы, добавляем значение в Map для входного объекта
            findAuthDirectiveAndGetRule(inputObject.getDirectives())
                    .ifPresent(rule -> addRuleToMap(inputObjectRules,
                            InputObjectInfo.newInputObjectInfo(inputObject.getName()), rule));
            //добавляем выражения для всех полей входного объекта
            inputObject.getInputValueDefinitions().forEach(inputValue ->
                    addRuleFromInputFieldDefinition(inputObject.getName(), inputValue));
        }

        /**
         * Добавляет строковые выражения для выходных объектов
         *
         * @param object определение выходного объекта
         */
        private void setRulesFromObject(ObjectTypeDefinition object) {
            //если есть выражение из директивы, добавляем значение в Map для выходного объекта
            findAuthDirectiveAndGetRule(object.getDirectives())
                    .ifPresent(rule -> addRuleToMap(objectRules, ObjectInfo.newObjectInfo(object.getName()), rule));
            //добавляем выражения для всех полей выходного объекта
            object.getFieldDefinitions().forEach(field -> addRuleFromField(object.getName(), field));
        }

        /**
         * Добавляет строковое выражение для поля входного объекта
         *
         * @param typeName   название родительского типа
         * @param inputField определение поля входного объекта
         */
        private void addRuleFromInputFieldDefinition(String typeName, InputValueDefinition inputField) {
            //если есть выражение из директивы, добавляем значение в Map для поля
            getRuleFromInputValue(inputField).ifPresent(rule ->
                    addRuleToMap(inputFieldRules,
                            InputFieldInfo.newInputFieldInfo(typeName, inputField.getName()), rule));
        }

        /**
         * Добавляет строковое выражение для поля
         *
         * @param typeName название родительского типа
         * @param field    определение поля
         */
        private void addRuleFromField(String typeName, FieldDefinition field) {
            //если есть выражение из директивы, добавляем значение в Map для поля
            findAuthDirectiveAndGetRule(field.getDirectives()).ifPresent(rule ->
                    addRuleToMap(fieldRules, FieldInfo.newFieldInfo(typeName, field.getName()), rule));
            //добавляем выражения у аргументов поля
            field.getInputValueDefinitions()
                    .forEach(inputValue -> addRuleFromArgument(typeName, field.getName(), inputValue));
        }

        /**
         * Добавляет строковое выражение для аргумента
         *
         * @param typeName  название родительского типа
         * @param fieldName название поля
         * @param argument  определение аргумента
         */
        private void addRuleFromArgument(String typeName, String fieldName, InputValueDefinition argument) {
            //если есть выражение из директивы, добавляем значение в Map для аргумента
            getRuleFromInputValue(argument).ifPresent(rule ->
                    addRuleToMap(argumentRules,
                            ArgumentInfo.newArgumentInfo(typeName, fieldName, argument.getName()), rule));
        }

        /**
         * Получает строковое выражение для аргумента или поля входного объекта
         *
         * @param inputValue определение аргумента или поля входного объекта
         * @return {@link Optional} со строковым выражением, {@link Optional#empty()} если директива не найдена
         */
        private Optional<StringExpressionRule> getRuleFromInputValue(InputValueDefinition inputValue) {
            return findAuthDirectiveAndGetRule(inputValue.getDirectives());
        }

        /**
         * Добавляет строковое выражение в {@link Map} для объекта применения
         *
         * @param map  {@link Map} для объекта применения
         * @param key  объект применения
         * @param rule строковое выражение
         * @param <T>  тип объекта применения
         */
        private <T> void addRuleToMap(Map<T, List<StringExpressionRule>> map, T key, StringExpressionRule rule) {
            map.putIfAbsent(key, new ArrayList<>());
            map.get(key).add(rule);
        }

        /**
         * Находит директиву auth и получает из нее строковое выражение
         *
         * @param directives список директив
         * @return {@link Optional} со строковым выражением, {@link Optional#empty()} если директива не найдена
         */
        private Optional<StringExpressionRule> findAuthDirectiveAndGetRule(List<Directive> directives) {
            return directives.stream()
                    .filter(directive -> AUTH_DIRECTIVE_NAME.equals(directive.getName()))
                    .findFirst()
                    .map(this::getFromDirective);
        }

        /**
         * Получает строковое выражение из директивы
         *
         * @param directive директива над объектом схемы
         * @return строковое выражение
         */
        private StringExpressionRule getFromDirective(Directive directive) {
            StringExpressionRule.Builder ruleBuilder = StringExpressionRule.newRule();
            //добавляем правила доступа из аргументов директивы
            directive.getArguments().stream()
                    .filter(argument -> argument.getValue() instanceof StringValue)
                    .forEach(argument -> setRuleFromDirectiveArgument(ruleBuilder, argument));
            return ruleBuilder.build();
        }

        /**
         * Добавляет строковое правило в builder {@link StringExpressionRule} из аргумента директивы
         *
         * @param ruleBuilder builder {@link StringExpressionRule}, куда нужно добавить правило
         * @param argument    аргумент директивы
         */
        private void setRuleFromDirectiveArgument(StringExpressionRule.Builder ruleBuilder, Argument argument) {
            String name = argument.getName();
            String value = ((StringValue) argument.getValue()).getValue();
            //если пустое выражение, не добавляем ничего
            if (isNullOrEmpty(value)) {
                return;
            }
            //добавляем соответсвующее правило в зависимости от названия аргумента директивы
            if ("r".equals(name)) {
                ruleBuilder.readRule(value);
            } else if ("w".equals(name)) {
                ruleBuilder.writeRule(value);
            } else if ("rw".equals(name)) {
                ruleBuilder.readWriteRule(value);
            }
        }

        /**
         * Преобразует {@link Map} со списками строковых выражений в {@link Map} с выражениями в объектном виде
         *
         * @param stringExpressionRuleMap {@link Map} объект применения -> список выражений в строковом виде
         * @param <T>                     тип объекта применения
         * @return {@link Map} объект применения -> выражение в объектном виде
         */
        private <T extends RuleTargetInfo> Map<T, TokenExpressionRule> transformRuleMap(
                Map<T, List<StringExpressionRule>> stringExpressionRuleMap) {
            Map<T, TokenExpressionRule> result = new HashMap<>();
            stringExpressionRuleMap.forEach((targetInfo, stringRules) ->
                    transform(stringRules, targetInfo)
                            .ifPresent(rule -> result.put(targetInfo, rule)));
            return result;
        }

        /**
         * Преобразует строковые выражения в одно выражение в объектном виде
         *
         * @param rules      строковые выражения
         * @param targetInfo информация об объекте применения правила
         * @return {@link Optional} с выражением в объектном виде или {@link Optional#empty()}, если правила пустые
         */
        private Optional<TokenExpressionRule> transform(List<StringExpressionRule> rules, RuleTargetInfo targetInfo) {
            //если правила пустые, возвращаем пустой Optional
            if (rules == null || rules.isEmpty()) {
                logger.debug("Can't transform. Rules of {} is empty", targetInfo);
                return Optional.empty();
            }

            logger.debug("Started transforming rules {} of {}", rules, targetInfo);

            //устанавливаем объект правила
            TokenExpressionRule.Builder builder = TokenExpressionRule.builder();
            builder.targetInfo(targetInfo);

            //получаем выражения в rpn из правил по типу операции
            List<TokenExpression> readRpnExpressions = new ArrayList<>();
            List<TokenExpression> writeRpnExpressions = new ArrayList<>();
            List<TokenExpression> readWriteRpnExpressions = new ArrayList<>();
            getRpnExpressions(readRpnExpressions, writeRpnExpressions, readWriteRpnExpressions, rules);

            //комбинируем readWrite выражения в одно
            TokenExpression readWriteExpression = tokenExpressionCombiner.combine(readWriteRpnExpressions);

            //устанавливаем read и write правила, предварительно скомбинировав с readWrite и минимизировав
            TokenExpressionRule tokenExpressionRule = builder
                    .readRule(combineReadOrWriteAndSimplify(readRpnExpressions, readWriteExpression))
                    .writeRule(combineReadOrWriteAndSimplify(writeRpnExpressions, readWriteExpression))
                    .build();

            logger.debug("Ended transforming rules {} of {}. Result is {}", rules, targetInfo, tokenExpressionRule);
            return Optional.of(tokenExpressionRule);
        }

        /**
         * Добавляет распарсенные строковые выражения в списки выражений чтения, записи и чтения/записи
         *
         * @param read      выражения чтения
         * @param write     выражения записи
         * @param readWrite выражения чтения/записи
         * @param rules     правила в строковом виде
         */
        private void getRpnExpressions(List<TokenExpression> read, List<TokenExpression> write,
                                       List<TokenExpression> readWrite, List<StringExpressionRule> rules) {
            rules.forEach(rule -> {
                parseAndConvertToRpnAndAdd(read, rule.getReadRule());
                parseAndConvertToRpnAndAdd(write, rule.getWriteRule());
                parseAndConvertToRpnAndAdd(readWrite, rule.getReadWriteRule());
            });
        }

        /**
         * Комбинирует выражения для чтения или записи с выражением чтения/записи и минифицирует
         *
         * @param toCombine         комбинируемые выражения для чтения или записи
         * @param combinedReadWrite выражение чтения/записи
         * @return скомбинированное и минифицированное выражение чтения или записи
         */
        private TokenExpression combineReadOrWriteAndSimplify(List<TokenExpression> toCombine,
                                                              TokenExpression combinedReadWrite) {
            TokenExpression combinedSimpleOperation = tokenExpressionCombiner.combine(toCombine);
            TokenExpression withReadWrite = tokenExpressionCombiner.combine(combinedSimpleOperation, combinedReadWrite);
            if (withReadWrite.isEmpty()) {
                return withReadWrite;
            }
            return expressionSimplifier.simplify(withReadWrite);
        }

        /**
         * Добавляет строковое правило в список выражений
         *
         * @param converted список выражений
         * @param rule      строкое выражение
         */
        private void parseAndConvertToRpnAndAdd(List<TokenExpression> converted, String rule) {
            if (!isNullOrEmpty(rule)) {
                converted.add(rpnExpressionConverter.convertToRpn(expressionParser.parse(rule)));
            }
        }
    }
}
