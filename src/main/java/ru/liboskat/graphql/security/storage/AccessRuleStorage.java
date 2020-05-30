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

public class AccessRuleStorage {
    private static final Logger logger = LoggerFactory.getLogger(AccessRuleStorage.class);

    public static final String AUTH_DIRECTIVE_NAME = "auth";

    private final TokenExpressionRule schemaRule;
    private final Map<ObjectInfo, TokenExpressionRule> objectRules;
    private final Map<FieldInfo, TokenExpressionRule> fieldRules;
    private final Map<ArgumentInfo, TokenExpressionRule> argumentRules;
    private final Map<InputObjectInfo, TokenExpressionRule> inputObjectRules;
    private final Map<InputFieldInfo, TokenExpressionRule> inputFieldRules;

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

    public static AccessRuleStorage.Builder newAccessRuleStorage() {
        return new Builder();
    }

    public Optional<TokenExpressionRule> getSchemaRule() {
        return Optional.ofNullable(schemaRule);
    }

    public Optional<TokenExpressionRule> getObjectRule(String objectName) {
        return Optional.ofNullable(objectRules.get(ObjectInfo.newObjectInfo(objectName)));
    }

    public Optional<TokenExpressionRule> getFieldRule(String objectName, String fieldName) {
        return Optional.ofNullable(fieldRules.get(FieldInfo.newFieldInfo(objectName, fieldName)));
    }

    public Optional<TokenExpressionRule> getArgumentRule(String objectName, String fieldName, String argumentName) {
        return Optional.ofNullable(argumentRules.get(ArgumentInfo.newArgumentInfo(objectName, fieldName, argumentName)));
    }

    public Optional<TokenExpressionRule> getInputObjectRule(String inputObjectName) {
        return Optional.ofNullable(inputObjectRules.get(InputObjectInfo.newInputObjectInfo(inputObjectName)));
    }

    public Optional<TokenExpressionRule> getInputFieldRule(String inputObjectName, String inputFieldName) {
        return Optional.ofNullable(inputFieldRules.get(InputFieldInfo.newInputFieldInfo(inputObjectName, inputFieldName)));
    }

    public static class Builder {
        private static final List<String> DIRECTIVE_INPUT_VALUE_NAMES = Arrays.asList("rw", "r", "w");
        private static final List<String> DIRECTIVE_LOCATIONS = Arrays.asList(
                Introspection.DirectiveLocation.SCHEMA.toString(),
                Introspection.DirectiveLocation.OBJECT.toString(),
                Introspection.DirectiveLocation.FIELD_DEFINITION.toString(),
                Introspection.DirectiveLocation.ARGUMENT_DEFINITION.toString(),
                Introspection.DirectiveLocation.INPUT_OBJECT.toString(),
                Introspection.DirectiveLocation.INPUT_FIELD_DEFINITION.toString()
        );
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
         * @param rule rules in string format
         * @return this builder
         * @throws IllegalArgumentException if rule is null or empty
         */
        public Builder schemaRule(StringExpressionRule rule) {
            throwExceptionIfNullOrEmpty(rule);
            schemaRules.add(rule);
            return this;
        }

        /**
         * Adds object rule
         *
         * @param rule       rules in string format
         * @param objectName name of object
         * @return this builder
         * @throws IllegalArgumentException if rule or objectName is null or empty
         */
        public Builder objectRule(StringExpressionRule rule, String objectName) {
            throwExceptionIfNullOrEmpty(rule);
            addRuleToMap(objectRules, ObjectInfo.newObjectInfo(objectName), rule);
            return this;
        }

        /**
         * Adds object field rule
         *
         * @param rule      rules in string format
         * @param typeName  name of parent object
         * @param fieldName name of field
         * @return this builder
         * @throws IllegalArgumentException if rule or typeName or fieldName is null or empty
         */
        public Builder fieldRule(StringExpressionRule rule, String typeName, String fieldName) {
            throwExceptionIfNullOrEmpty(rule);
            addRuleToMap(fieldRules, FieldInfo.newFieldInfo(typeName, fieldName), rule);
            return this;
        }

        /**
         * Add field argument rule
         *
         * @param rule         rules in string format
         * @param typeName     name of parent object
         * @param fieldName    name of field
         * @param argumentName name of argument
         * @return this builder
         * @throws IllegalArgumentException if rule or typeName or fieldName or argumentName is null or empty
         */
        public Builder argumentRule(StringExpressionRule rule, String typeName, String fieldName, String argumentName) {
            throwExceptionIfNullOrEmpty(rule);
            addRuleToMap(argumentRules, ArgumentInfo.newArgumentInfo(typeName, fieldName, argumentName), rule);
            return this;
        }

        /**
         * Adds input object rule
         *
         * @param rule            rules in string format
         * @param inputObjectName name of input object
         * @return this builder
         * @throws IllegalArgumentException if rule or inputObjectName is null or empty
         */
        public Builder inputObjectRule(StringExpressionRule rule, String inputObjectName) {
            throwExceptionIfNullOrEmpty(rule);
            addRuleToMap(inputObjectRules, InputObjectInfo.newInputObjectInfo(inputObjectName), rule);
            return this;
        }

        /**
         * Adds object field rule
         *
         * @param rule          rules in string format
         * @param inputTypeName name of parent input object
         * @param fieldName     name of field
         * @return this builder
         * @throws IllegalArgumentException if rule or inputTypeName or fieldName is null or empty
         */
        public Builder inputFieldRule(StringExpressionRule rule, String inputTypeName, String fieldName) {
            throwExceptionIfNullOrEmpty(rule);
            addRuleToMap(inputFieldRules, InputFieldInfo.newInputFieldInfo(inputTypeName, fieldName), rule);
            return this;
        }

        private void throwExceptionIfNullOrEmpty(StringExpressionRule rule) {
            if (rule == null) {
                throw new IllegalArgumentException("Rule can't be null or empty");
            }
        }

        /**
         * Adds all rules from {@link TypeDefinitionRegistry} of GraphQL Schema
         *
         * @param registry {@link TypeDefinitionRegistry} of GraphQL Schema
         * @return this builder
         * @throws InvalidAuthDirectiveException if declared @auth directive is incorrect
         */
        public Builder fromTypeDefinitionRegistry(TypeDefinitionRegistry registry) {
            Optional<DirectiveDefinition> authDirectiveOptional = registry.getDirectiveDefinition(AUTH_DIRECTIVE_NAME);
            authDirectiveOptional.ifPresent(authDirective -> {
                checkAuthDirectiveDefinition(authDirective);
                hasDirective = true;
            });
            registry.schemaDefinition().ifPresent(this::setRuleFromSchemaDefinition);
            registry.types().values().forEach(this::setRulesFromType);
            if (!hasDirective) {
                registry.add(buildAuthDirectiveDefinition());
            }
            return this;
        }

        /**
         * @return constructed {@link AccessRuleStorage}
         */
        public AccessRuleStorage build() {
            logger.debug("AccessRuleStorage building started");
            Map<ObjectInfo, TokenExpressionRule> objectRules = transformRuleMap(this.objectRules);
            Map<FieldInfo, TokenExpressionRule> fieldRules = transformRuleMap(this.fieldRules);
            Map<ArgumentInfo, TokenExpressionRule> argumentRules = transformRuleMap(this.argumentRules);
            Map<InputObjectInfo, TokenExpressionRule> inputObjectRules = transformRuleMap(this.inputObjectRules);
            Map<InputFieldInfo, TokenExpressionRule> inputFieldRules = transformRuleMap(this.inputFieldRules);
            if (!schemaRules.isEmpty()) {
                Optional<TokenExpressionRule> schemaRuleOptional = transform(schemaRules, SchemaInfo.newSchemaInfo());
                if (schemaRuleOptional.isPresent()) {
                    return new AccessRuleStorage(schemaRuleOptional.get(), objectRules, fieldRules, argumentRules,
                            inputObjectRules, inputFieldRules);
                }
            }
            AccessRuleStorage accessRuleStorage = new AccessRuleStorage(objectRules, fieldRules, argumentRules,
                    inputObjectRules, inputFieldRules);
            logger.debug("AccessRuleStorage building ended");
            return accessRuleStorage;
        }

        private void checkAuthDirectiveDefinition(DirectiveDefinition directiveDefinition) {
            List<InputValueDefinition> inputValueDefinitions = directiveDefinition.getInputValueDefinitions();
            List<DirectiveLocation> directiveLocations = directiveDefinition.getDirectiveLocations();
            boolean correct = AUTH_DIRECTIVE_NAME.equals(directiveDefinition.getName()) &&
                    DIRECTIVE_INPUT_VALUE_NAMES.size() >= inputValueDefinitions.size() &&
                    inputValueDefinitions.stream()
                            .map(InputValueDefinition::getType)
                            .allMatch(type -> type instanceof TypeName &&
                                    DIRECTIVE_INPUT_TYPE_NAME.equals(((TypeName) type).getName())) &&
                    inputValueDefinitions.stream()
                            .map(InputValueDefinition::getName)
                            .allMatch(DIRECTIVE_INPUT_VALUE_NAMES::contains) &&
                    DIRECTIVE_LOCATIONS.size() >= directiveLocations.size() &&
                    directiveLocations.stream()
                            .map(DirectiveLocation::getName)
                            .allMatch(DIRECTIVE_LOCATIONS::contains);
            if (!correct) {
                throw new InvalidAuthDirectiveException();
            }
        }

        private DirectiveDefinition buildAuthDirectiveDefinition() {
            DirectiveDefinition.Builder directiveBuilder = DirectiveDefinition.newDirectiveDefinition();
            directiveBuilder.name(AUTH_DIRECTIVE_NAME);
            DIRECTIVE_INPUT_VALUE_NAMES.forEach(inputValueName ->
                    directiveBuilder.inputValueDefinition(InputValueDefinition.newInputValueDefinition()
                            .name(inputValueName)
                            .type(TypeName.newTypeName(DIRECTIVE_INPUT_TYPE_NAME).build())
                            .build()
                    ));
            DIRECTIVE_LOCATIONS.forEach(locationName ->
                    directiveBuilder.directiveLocation(DirectiveLocation.newDirectiveLocation()
                            .name(locationName)
                            .build()));
            return directiveBuilder.build();
        }

        private void setRuleFromSchemaDefinition(SchemaDefinition schema) {
            getFromDirectives(schema.getDirectives()).ifPresent(schemaRules::add);
        }

        private void setRulesFromType(TypeDefinition<?> type) {
            if (type instanceof InputObjectTypeDefinition) {
                InputObjectTypeDefinition inputObject = (InputObjectTypeDefinition) type;
                getFromDirectives(inputObject.getDirectives())
                        .ifPresent(rule -> addRuleToMap(inputObjectRules,
                                InputObjectInfo.newInputObjectInfo(type.getName()), rule));
                inputObject.getInputValueDefinitions().forEach(inputValue ->
                        addRuleFromInputFieldDefinition(inputObject.getName(), inputValue));
            } else if (type instanceof ObjectTypeDefinition) {
                ObjectTypeDefinition object = (ObjectTypeDefinition) type;
                getFromDirectives(object.getDirectives())
                        .ifPresent(rule -> addRuleToMap(objectRules, ObjectInfo.newObjectInfo(type.getName()), rule));
                object.getFieldDefinitions().forEach(field -> addRuleFromField(object.getName(), field));
            }
        }

        private void addRuleFromInputFieldDefinition(String typeName, InputValueDefinition inputField) {
            getRuleFromInputValue(inputField).ifPresent(rule ->
                    addRuleToMap(inputFieldRules,
                            InputFieldInfo.newInputFieldInfo(typeName, inputField.getName()), rule));
        }

        private void addRuleFromField(String typeName, FieldDefinition field) {
            getFromDirectives(field.getDirectives()).ifPresent(rule ->
                    addRuleToMap(fieldRules, FieldInfo.newFieldInfo(typeName, field.getName()), rule));
            field.getInputValueDefinitions()
                    .forEach(inputValue -> addRuleFromArgument(typeName, field.getName(), inputValue));
        }

        private void addRuleFromArgument(String typeName, String fieldName, InputValueDefinition argument) {
            getRuleFromInputValue(argument).ifPresent(rule ->
                    addRuleToMap(argumentRules,
                            ArgumentInfo.newArgumentInfo(typeName, fieldName, argument.getName()), rule));
        }

        private Optional<StringExpressionRule> getRuleFromInputValue(InputValueDefinition inputValue) {
            return getFromDirectives(inputValue.getDirectives());
        }

        private <T> void addRuleToMap(Map<T, List<StringExpressionRule>> map, T key, StringExpressionRule rule) {
            map.putIfAbsent(key, new ArrayList<>());
            map.get(key).add(rule);
        }

        private Optional<StringExpressionRule> getFromDirectives(List<Directive> directives) {
            return directives.stream()
                    .filter(directive -> AUTH_DIRECTIVE_NAME.equals(directive.getName()))
                    .findFirst()
                    .map(directive -> {
                        StringExpressionRule.Builder ruleBuilder = StringExpressionRule.newRule();
                        directive.getArguments().stream()
                                .filter(argument -> argument.getValue() instanceof StringValue)
                                .forEach(argument -> setRuleFromDirectiveArgument(ruleBuilder, argument));
                        return ruleBuilder.build();
                    });
        }

        private void setRuleFromDirectiveArgument(StringExpressionRule.Builder ruleBuilder, Argument argument) {
            String name = argument.getName();
            String value = ((StringValue) argument.getValue()).getValue();
            if (!isNullOrEmpty(value)) {
                if ("r".equals(name)) {
                    ruleBuilder.readRule(value);
                } else if ("w".equals(name)) {
                    ruleBuilder.writeRule(value);
                } else if ("rw".equals(name)) {
                    ruleBuilder.readWriteRule(value);
                }
            }
        }

        private <T extends RuleTargetInfo> Map<T, TokenExpressionRule> transformRuleMap(
                Map<T, List<StringExpressionRule>> stringExpressionRuleMap) {
            Map<T, TokenExpressionRule> result = new HashMap<>();
            stringExpressionRuleMap.forEach((targetInfo, stringRules) ->
                    transform(stringRules, targetInfo)
                            .ifPresent(rule -> result.put(targetInfo, rule)));
            return result;
        }

        private Optional<TokenExpressionRule> transform(List<StringExpressionRule> rules, RuleTargetInfo targetInfo) {
            logger.debug("Started transforming rules {} of {}", rules, targetInfo);
            TokenExpressionRule.Builder builder = TokenExpressionRule.builder();
            builder.targetInfo(targetInfo);
            if (rules == null || rules.isEmpty()) {
                logger.debug("Can't transform. Rules of {} is empty", targetInfo);
                return Optional.empty();
            }
            List<TokenExpression> readRpnExpressions = new ArrayList<>();
            List<TokenExpression> writeRpnExpressions = new ArrayList<>();
            List<TokenExpression> readWriteRpnExpressions = new ArrayList<>();
            rules.forEach(rule -> {
                parseAndConvertToRpnAndAdd(readRpnExpressions, rule.getReadRule());
                parseAndConvertToRpnAndAdd(writeRpnExpressions, rule.getWriteRule());
                parseAndConvertToRpnAndAdd(readWriteRpnExpressions, rule.getReadWriteRule());
            });
            TokenExpression readExpression = tokenExpressionCombiner.combine(readRpnExpressions);
            TokenExpression writeExpression = tokenExpressionCombiner.combine(writeRpnExpressions);
            TokenExpression readWriteExpression = tokenExpressionCombiner.combine(readWriteRpnExpressions);
            readExpression = tokenExpressionCombiner.combine(readExpression, readWriteExpression);
            writeExpression = tokenExpressionCombiner.combine(writeExpression, readWriteExpression);
            if (!readExpression.isEmpty()) {
                readExpression = expressionSimplifier.simplify(readExpression);
            }
            if (!writeExpression.isEmpty()) {
                writeExpression = expressionSimplifier.simplify(writeExpression);
            }
            TokenExpressionRule tokenExpressionRule = builder
                    .readRule(readExpression)
                    .writeRule(writeExpression)
                    .build();
            logger.debug("Ended transforming rules {} of {}. Result is {}", rules, targetInfo, tokenExpressionRule);
            return Optional.of(tokenExpressionRule);
        }

        private void parseAndConvertToRpnAndAdd(List<TokenExpression> converted, String rule) {
            if (!isNullOrEmpty(rule)) {
                converted.add(rpnExpressionConverter.convertToRpn(expressionParser.parse(rule)));
            }
        }
    }
}
