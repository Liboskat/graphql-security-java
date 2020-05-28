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

import java.util.*;

/**
 * This class is used to store rules of schema, objects, fields, arguments, input objects, input fields
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

    public Optional<TokenExpressionRule> getSchemaRule() {
        return Optional.ofNullable(schemaRule);
    }

    public Optional<TokenExpressionRule> getObjectRule(String objectName) {
        return Optional.ofNullable(objectRules.get(new ObjectInfo(objectName)));
    }

    public Optional<TokenExpressionRule> getFieldRule(String objectName, String fieldName) {
        return Optional.ofNullable(fieldRules.get(new FieldInfo(objectName, fieldName)));
    }

    public Optional<TokenExpressionRule> getArgumentRule(String objectName, String fieldName, String argumentName) {
        return Optional.ofNullable(argumentRules.get(new ArgumentInfo(objectName, fieldName, argumentName)));
    }

    public Optional<TokenExpressionRule> getInputObjectRule(String inputObjectName) {
        return Optional.ofNullable(inputObjectRules.get(new InputObjectInfo(inputObjectName)));
    }

    public Optional<TokenExpressionRule> getInputFieldRule(String inputObjectName, String inputFieldName) {
        return Optional.ofNullable(inputFieldRules.get(new InputFieldInfo(inputObjectName, inputFieldName)));
    }

    /**
     * @return new {@link Builder} instance
     */
    public static AccessRuleStorage.Builder newAccessRuleStorage() {
        return new Builder();
    }

    /**
     * Interface marking that class is information about rule target
     */
    public interface RuleTargetInfo {
    }

    /**
     * Class that shows that target is schema
     */
    public static class SchemaInfo implements RuleTargetInfo {
        @Override
        public String toString() {
            return "Schema";
        }
    }

    /**
     * Class with information about object
     */
    public static class ObjectInfo implements RuleTargetInfo {
        private String name;

        ObjectInfo(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Object name can't be null or empty");
            }
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ObjectInfo that = (ObjectInfo) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return "Object{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

    /**
     * Class with information about field
     */
    public static class FieldInfo implements RuleTargetInfo {
        private String typeName;
        private String fieldName;

        FieldInfo(String typeName, String fieldName) {
            if (typeName == null || typeName.isEmpty() || fieldName == null || fieldName.isEmpty()) {
                throw new IllegalArgumentException("TypeName and fieldName can't be null or empty");
            }
            this.typeName = typeName;
            this.fieldName = fieldName;
        }

        public String getTypeName() {
            return typeName;
        }

        public String getFieldName() {
            return fieldName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FieldInfo that = (FieldInfo) o;
            return typeName.equals(that.typeName) && fieldName.equals(that.fieldName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeName, fieldName);
        }

        @Override
        public String toString() {
            return "Field{" +
                    "parent type='" + typeName + '\'' +
                    ", name='" + fieldName + '\'' +
                    '}';
        }
    }

    /**
     * Class with information about argument
     */
    public static class ArgumentInfo implements RuleTargetInfo {
        private String typeName;
        private String fieldName;
        private String argumentName;

        ArgumentInfo(String typeName, String fieldName, String argumentName) {
            if (typeName == null || typeName.isEmpty() || fieldName == null || fieldName.isEmpty() ||
                    argumentName == null || argumentName.isEmpty()) {
                throw new IllegalArgumentException("TypeName, fieldName and argumentName can't be null or empty");
            }
            this.typeName = typeName;
            this.fieldName = fieldName;
            this.argumentName = argumentName;
        }

        public String getTypeName() {
            return typeName;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getArgumentName() {
            return argumentName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArgumentInfo that = (ArgumentInfo) o;
            return typeName.equals(that.typeName) && fieldName.equals(that.fieldName) &&
                    argumentName.equals(that.argumentName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeName, fieldName, argumentName);
        }

        @Override
        public String toString() {
            return "Argument{" +
                    "parent type='" + typeName + '\'' +
                    ", field='" + fieldName + '\'' +
                    ", name='" + argumentName + '\'' +
                    '}';
        }
    }

    /**
     * Class with information about input object
     */
    public static class InputObjectInfo implements RuleTargetInfo {
        private String name;

        InputObjectInfo(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("InputObject name can't be null or empty");
            }
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InputObjectInfo that = (InputObjectInfo) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return "Input{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

    /**
     * Class with information about input object field
     */
    public static class InputFieldInfo implements RuleTargetInfo {
        private String inputTypeName;
        private String fieldName;

        InputFieldInfo(String inputTypeName, String fieldName) {
            if (inputTypeName == null || inputTypeName.isEmpty() || fieldName == null || fieldName.isEmpty()) {
                throw new IllegalArgumentException("InputTypeName and fieldName can't be null or empty");
            }
            this.inputTypeName = inputTypeName;
            this.fieldName = fieldName;
        }

        public String getInputTypeName() {
            return inputTypeName;
        }

        public String getFieldName() {
            return fieldName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InputFieldInfo that = (InputFieldInfo) o;
            return inputTypeName.equals(that.inputTypeName) && fieldName.equals(that.fieldName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(inputTypeName, fieldName);
        }

        @Override
        public String toString() {
            return "Input field{" +
                    "parent input type='" + inputTypeName + '\'' +
                    ", name='" + fieldName + '\'' +
                    '}';
        }
    }

    /**
     * This class is used to construct {@link AccessRuleStorage}
     */
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

        boolean hasDirective;
        private final List<StringExpressionRule> schemaRules;
        private final Map<ObjectInfo, List<StringExpressionRule>> objectRules;
        private final Map<FieldInfo, List<StringExpressionRule>> fieldRules;
        private final Map<ArgumentInfo, List<StringExpressionRule>> argumentRules;
        private final Map<InputObjectInfo, List<StringExpressionRule>> inputObjectRules;
        private final Map<InputFieldInfo, List<StringExpressionRule>> inputFieldRules;

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
         * Adds schema rule
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
         * @param rule rules in string format
         * @param objectName name of object
         * @return this builder
         * @throws IllegalArgumentException if rule or objectName is null or empty
         */
        public Builder objectRule(StringExpressionRule rule, String objectName) {
            throwExceptionIfNullOrEmpty(rule);
            addRuleToMap(objectRules, new ObjectInfo(objectName), rule);
            return this;
        }

        /**
         * Adds object field rule
         * @param rule rules in string format
         * @param typeName name of parent object
         * @param fieldName name of field
         * @return this builder
         * @throws IllegalArgumentException if rule or typeName or fieldName is null or empty
         */
        public Builder fieldRule(StringExpressionRule rule, String typeName, String fieldName) {
            throwExceptionIfNullOrEmpty(rule);
            addRuleToMap(fieldRules, new FieldInfo(typeName, fieldName), rule);
            return this;
        }

        /**
         * Add field argument rule
         * @param rule rules in string format
         * @param typeName name of parent object
         * @param fieldName name of field
         * @param argumentName name of argument
         * @return this builder
         * @throws IllegalArgumentException if rule or typeName or fieldName or argumentName is null or empty
         */
        public Builder argumentRule(StringExpressionRule rule, String typeName, String fieldName, String argumentName) {
            throwExceptionIfNullOrEmpty(rule);
            addRuleToMap(argumentRules, new ArgumentInfo(typeName, fieldName, argumentName), rule);
            return this;
        }

        /**
         * Adds input object rule
         * @param rule rules in string format
         * @param inputObjectName name of input object
         * @return this builder
         * @throws IllegalArgumentException if rule or inputObjectName is null or empty
         */
        public Builder inputObjectRule(StringExpressionRule rule, String inputObjectName) {
            throwExceptionIfNullOrEmpty(rule);
            addRuleToMap(inputObjectRules, new InputObjectInfo(inputObjectName), rule);
            return this;
        }

        /**
         * Adds object field rule
         * @param rule rules in string format
         * @param inputTypeName name of parent input object
         * @param fieldName name of field
         * @return this builder
         * @throws IllegalArgumentException if rule or inputTypeName or fieldName is null or empty
         */
        public Builder inputFieldRule(StringExpressionRule rule, String inputTypeName, String fieldName) {
            throwExceptionIfNullOrEmpty(rule);
            addRuleToMap(inputFieldRules, new InputFieldInfo(inputTypeName, fieldName), rule);
            return this;
        }

        private void throwExceptionIfNullOrEmpty(StringExpressionRule rule) {
            if (rule == null) {
                throw new IllegalArgumentException("Rule can't be null or empty");
            }
        }

        /**
         * Adds all rules from {@link TypeDefinitionRegistry} of GraphQL Schema
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
                Optional<TokenExpressionRule> schemaRuleOptional =  transform(schemaRules, new SchemaInfo());
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

        private void setRulesFromType(TypeDefinition type) {
            if (type instanceof InputObjectTypeDefinition) {
                InputObjectTypeDefinition inputObject = (InputObjectTypeDefinition) type;
                getFromDirectives(inputObject.getDirectives())
                        .ifPresent(rule -> addRuleToMap(inputObjectRules, new InputObjectInfo(type.getName()), rule));
                inputObject.getInputValueDefinitions().forEach(inputValue ->
                        addRuleFromInputFieldDefinition(inputObject.getName(), inputValue));
            } else if (type instanceof ObjectTypeDefinition) {
                ObjectTypeDefinition object = (ObjectTypeDefinition) type;
                getFromDirectives(object.getDirectives())
                        .ifPresent(rule -> addRuleToMap(objectRules, new ObjectInfo(type.getName()), rule));
                object.getFieldDefinitions().forEach(field -> addRuleFromField(object.getName(), field));
            }
        }

        private void addRuleFromInputFieldDefinition(String typeName, InputValueDefinition inputField) {
            getRuleFromInputValue(inputField).ifPresent(rule ->
                    addRuleToMap(inputFieldRules, new InputFieldInfo(typeName, inputField.getName()), rule));
        }

        private void addRuleFromField(String typeName, FieldDefinition field) {
            getFromDirectives(field.getDirectives()).ifPresent(rule ->
                    addRuleToMap(fieldRules, new FieldInfo(typeName, field.getName()), rule));
            field.getInputValueDefinitions()
                    .forEach(inputValue -> addRuleFromArgument(typeName, field.getName(), inputValue));
        }

        private void addRuleFromArgument(String typeName, String fieldName, InputValueDefinition argument) {
            getRuleFromInputValue(argument).ifPresent(rule ->
                    addRuleToMap(argumentRules, new ArgumentInfo(typeName, fieldName, argument.getName()), rule));
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
                        StringExpressionRule.Builder rule = StringExpressionRule.newRule();
                        directive.getArguments().stream()
                                .filter(argument -> argument.getValue() instanceof StringValue)
                                .forEach(argument -> {
                                    String name = argument.getName();
                                    String value = ((StringValue) argument.getValue()).getValue();
                                    if (value != null && !value.isEmpty()) {
                                        if ("r".equals(name)) {
                                            rule.readRule(value);
                                        } else if ("w".equals(name)) {
                                            rule.writeRule(value);
                                        } else if ("rw".equals(name)) {
                                            rule.readWriteRule(value);
                                        }
                                    }
                                });
                        return rule.build();
                    })
                    .filter(rule -> rule.getReadRule() != null || rule.getWriteRule() != null ||
                            rule.getReadWriteRule() != null);
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
                String readRule = rule.getReadRule();
                if (readRule != null && !readRule.isEmpty()) {
                    readRpnExpressions.add(
                            rpnExpressionConverter.convertToRpn(expressionParser.parse(readRule)));
                }
                String writeRule = rule.getWriteRule();
                if (writeRule != null && !writeRule.isEmpty()) {
                    writeRpnExpressions.add(
                            rpnExpressionConverter.convertToRpn(expressionParser.parse(writeRule)));
                }
                String readWriteRule = rule.getReadWriteRule();
                if (readWriteRule != null && !readWriteRule.isEmpty()) {
                    readWriteRpnExpressions.add(
                            rpnExpressionConverter.convertToRpn(expressionParser.parse(readWriteRule)));
                }
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
    }
}
