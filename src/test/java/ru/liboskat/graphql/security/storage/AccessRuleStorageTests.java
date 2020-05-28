package ru.liboskat.graphql.security.storage;

import graphql.Scalars;
import graphql.introspection.Introspection;
import graphql.language.DirectiveLocation;
import graphql.language.InputValueDefinition;
import graphql.language.TypeName;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.junit.jupiter.api.Test;
import ru.liboskat.graphql.security.exceptions.InvalidAuthDirectiveException;
import ru.liboskat.graphql.security.storage.AccessRuleStorage.*;
import ru.liboskat.graphql.security.storage.ComparisonToken.ComparisonType;
import ru.liboskat.graphql.security.storage.ComparisonToken.ValueType;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AccessRuleStorageTests {
    private static final Set<String> DIRECTIVE_INPUT_VALUE_NAMES = new HashSet<>(Arrays.asList("rw", "r", "w"));
    private static final Set<String> DIRECTIVE_LOCATIONS = new HashSet<>(Arrays.asList(
            Introspection.DirectiveLocation.SCHEMA.toString(),
            Introspection.DirectiveLocation.OBJECT.toString(),
            Introspection.DirectiveLocation.FIELD_DEFINITION.toString(),
            Introspection.DirectiveLocation.ARGUMENT_DEFINITION.toString(),
            Introspection.DirectiveLocation.INPUT_OBJECT.toString(),
            Introspection.DirectiveLocation.INPUT_FIELD_DEFINITION.toString()
    ));
    private static final String DIRECTIVE_INPUT_TYPE_NAME = Scalars.GraphQLString.getName();

    @Test
    void addSchemaRule_ReadWrite_shouldBeAdded() {
        AccessRuleStorage storage = AccessRuleStorage.newAccessRuleStorage()
                .schemaRule(StringExpressionRule.newRule()
                        .rw("rule = 'rule'")
                        .build())
                .build();
        TokenExpression expected = getOneTokenEqualityExpression("rule");
        Optional<TokenExpressionRule> rule = storage.getSchemaRule();
        assertTrue(rule.isPresent());
        assertEquals(expected, rule.orElseThrow(IllegalArgumentException::new).getWriteRule());
        assertEquals(expected, rule.orElseThrow(IllegalArgumentException::new).getReadRule());
    }

    @Test
    void addOutputObjectRule_Read_shouldBeAdded() {
        AccessRuleStorage storage = AccessRuleStorage.newAccessRuleStorage()
                .objectRule(StringExpressionRule.newRule()
                        .r("rule = 'rule'")
                        .build(), "object")
                .build();
        TokenExpression expected = getOneTokenEqualityExpression("rule");
        Optional<TokenExpressionRule> rule = storage.getObjectRule("object");
        assertTrue(rule.isPresent());
        assertEquals(expected, rule.orElseThrow(IllegalArgumentException::new).getReadRule());
    }

    @Test
    void addOutputObjectFieldRule_Read_shouldBeAdded() {
        AccessRuleStorage storage = AccessRuleStorage.newAccessRuleStorage()
                .fieldRule(StringExpressionRule.newRule()
                        .r("rule = 'rule'")
                        .build(), "object", "field")
                .build();
        TokenExpression expected = getOneTokenEqualityExpression("rule");
        Optional<TokenExpressionRule> rule = storage.getFieldRule("object", "field");
        assertTrue(rule.isPresent());
        assertEquals(expected, rule.orElseThrow(IllegalArgumentException::new).getReadRule());
    }

    @Test
    void addArgumentReadRule_Write_shouldBeAdded() {
        AccessRuleStorage storage = AccessRuleStorage.newAccessRuleStorage()
                .argumentRule(StringExpressionRule.newRule()
                        .rw("rule = 'rule'")
                        .build(), "object", "field", "arg")
                .build();
        TokenExpression expected = getOneTokenEqualityExpression("rule");
        Optional<TokenExpressionRule> rule = storage.getArgumentRule("object", "field", "arg");
        assertTrue(rule.isPresent());
        assertEquals(expected, rule.orElseThrow(IllegalArgumentException::new).getReadRule());
        assertEquals(expected, rule.orElseThrow(IllegalArgumentException::new).getWriteRule());
    }

    @Test
    void addInputObjectRule_Write_shouldBeAdded() {
        AccessRuleStorage storage = AccessRuleStorage.newAccessRuleStorage()
                .inputObjectRule(StringExpressionRule.newRule()
                        .w("rule = 'rule'")
                        .build(), "inputObject")
                .build();
        TokenExpression expected = getOneTokenEqualityExpression("rule");
        Optional<TokenExpressionRule> rule = storage.getInputObjectRule("inputObject");
        assertTrue(rule.isPresent());
        assertEquals(expected, rule.orElseThrow(IllegalArgumentException::new).getWriteRule());
    }

    @Test
    void addInputObjectFieldRule_Write_shouldBeAdded() {
        AccessRuleStorage storage = AccessRuleStorage.newAccessRuleStorage()
                .inputFieldRule(StringExpressionRule.newRule()
                        .w("rule = 'rule'")
                        .build(), "inputObject", "field")
                .build();
        TokenExpression expected = getOneTokenEqualityExpression("rule");
        Optional<TokenExpressionRule> rule = storage.getInputFieldRule("inputObject", "field");
        assertTrue(rule.isPresent());
        assertEquals(expected, rule.orElseThrow(IllegalArgumentException::new).getWriteRule());
    }

    @Test
    void add_fromTypeDefinitionRegistry_shouldBeAdded() throws URISyntaxException, IOException {
        AccessRuleStorage storage = AccessRuleStorage.newAccessRuleStorage()
                .fromTypeDefinitionRegistry(loadSchema("schema.graphqls"))
                .build();

        Optional<TokenExpressionRule> schemaRule = storage.getSchemaRule();
        Optional<TokenExpressionRule> outputObjectRule = storage.getObjectRule("OutputObjectFieldType");
        Optional<TokenExpressionRule> outputObjectFieldRule = storage
                .getFieldRule("OutputObjectFieldType", "field");
        Optional<TokenExpressionRule> argumentRule = storage
                .getArgumentRule("Mutation", "mutation", "argument");
        Optional<TokenExpressionRule> inputObjectRule = storage.getInputObjectRule("InputObjectFieldType");
        Optional<TokenExpressionRule> inputObjectFieldRule = storage
                .getInputFieldRule("InputObjectFieldType", "field");

        assertAll(
                () -> assertTrue(schemaRule.isPresent()),
                () -> assertTrue(outputObjectRule.isPresent()),
                () -> assertTrue(outputObjectFieldRule.isPresent()),
                () -> assertTrue(argumentRule.isPresent()),
                () -> assertTrue(inputObjectRule.isPresent()),
                () -> assertTrue(inputObjectFieldRule.isPresent()));

        TokenExpression schemaExpectedExpression = getOneTokenEqualityExpression("schema");
        TokenExpression outputObjectExpectedExpression = getOneTokenEqualityExpression("outputObjectFieldType");
        TokenExpression outputObjectFieldExpectedExpression = getOneTokenEqualityExpression("scalarField2");
        TokenExpression argumentExpectedExpression = getOneTokenEqualityExpression("input");
        TokenExpression inputObjectExpectedExpression = getOneTokenEqualityExpression("inputObjectFieldType");
        TokenExpression inputObjectFieldExpectedExpression = getOneTokenEqualityExpression("scalarField3");

        assertAll(
                () -> assertEquals(schemaExpectedExpression,
                        schemaRule.orElseThrow(IllegalArgumentException::new).getReadRule()),
                () -> assertEquals(schemaExpectedExpression,
                        schemaRule.orElseThrow(IllegalArgumentException::new).getWriteRule()),
                () -> assertEquals(outputObjectExpectedExpression,
                        outputObjectRule.orElseThrow(IllegalArgumentException::new).getReadRule()),
                () -> assertEquals(outputObjectFieldExpectedExpression,
                        outputObjectFieldRule.orElseThrow(IllegalArgumentException::new).getReadRule()),
                () -> assertEquals(argumentExpectedExpression,
                        argumentRule.orElseThrow(IllegalArgumentException::new).getWriteRule()),
                () -> assertEquals(inputObjectExpectedExpression,
                        inputObjectRule.orElseThrow(IllegalArgumentException::new).getWriteRule()),
                () -> assertEquals(inputObjectFieldExpectedExpression,
                        inputObjectFieldRule.orElseThrow(IllegalArgumentException::new).getWriteRule()));
    }

    @Test
    void addSchemaRule_Empty_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                AccessRuleStorage.newAccessRuleStorage()
                        .schemaRule(null)
                        .build());
    }

    @Test
    void addOutputObjectRule_Empty_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                AccessRuleStorage.newAccessRuleStorage()
                        .objectRule(null, "name")
                        .build());
    }

    @Test
    void addOutputObjectFieldRule_Empty_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                AccessRuleStorage.newAccessRuleStorage()
                        .fieldRule(null, "name", "name")
                        .build());
    }

    @Test
    void addArgumentRule_Empty_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                AccessRuleStorage.newAccessRuleStorage()
                        .argumentRule(null, "name", "name", "name")
                        .build());
    }

    @Test
    void addInputObjectRule_Empty_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                AccessRuleStorage.newAccessRuleStorage()
                        .inputObjectRule(null, "name")
                        .build());
    }

    @Test
    void addInputObjectFieldRule_Empty_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                AccessRuleStorage.newAccessRuleStorage()
                        .inputFieldRule(null, "name", "name")
                        .build());
    }

    @Test
    void equalObjectInfo_shouldBeEqual() {
        assertEquals(new ObjectInfo("object"), new ObjectInfo("object"));
    }

    @Test
    void unequalObjectInfo_shouldBeNotEqual() {
        assertNotEquals(new ObjectInfo("object"), new ObjectInfo("not same"));
    }

    @Test
    void equalFieldInfo_shouldBeEqual() {
        assertEquals(new FieldInfo("object", "field"), new FieldInfo("object", "field"));
    }

    @Test
    void unequalFieldInfo_shouldBeNotEqual() {
        assertNotEquals(new FieldInfo("object", "field"), new FieldInfo("object", "not same"));
    }

    @Test
    void equalArgumentInfo_shouldBeEqual() {
        assertEquals(new ArgumentInfo("object", "field", "arg"),
                new ArgumentInfo("object", "field", "arg"));
    }

    @Test
    void unequalArgumentInfo_shouldBeNotEqual() {
        assertNotEquals(new ArgumentInfo("object", "field", "arg"),
                new ArgumentInfo("object", "field", "not same"));
    }

    @Test
    void equalInputObjectInfo_shouldBeEqual() {
        assertEquals(new InputObjectInfo("object"), new InputObjectInfo("object"));
    }

    @Test
    void unequalInputObjectInfo_shouldBeNotEqual() {
        assertNotEquals(new InputObjectInfo("object"), new InputObjectInfo("not same"));
    }

    @Test
    void equalInputFieldInfo_shouldBeEqual() {
        assertEquals(new InputFieldInfo("object", "field"),
                new InputFieldInfo("object", "field"));
    }

    @Test
    void unequalInputFieldInfo_shouldBeNotEqual() {
        assertNotEquals(new InputFieldInfo("object", "field"),
                new InputFieldInfo("object", "not same"));
    }

    @Test
    void add_fromTypeDefinitionRegistry_withoutDirective_directiveShouldBeAdded() throws URISyntaxException, IOException {
        TypeDefinitionRegistry registry = loadSchema("schema_without_directive.graphqls");
        AccessRuleStorage.newAccessRuleStorage()
                .fromTypeDefinitionRegistry(registry)
                .build();
        assertTrue(registry.getDirectiveDefinition("auth")
                .filter(directive ->
                        DIRECTIVE_LOCATIONS.equals(directive.getDirectiveLocations()
                                .stream()
                                .map(DirectiveLocation::getName)
                                .collect(Collectors.toSet())))
                .filter(directive ->
                        DIRECTIVE_INPUT_VALUE_NAMES.equals(directive.getInputValueDefinitions()
                                .stream()
                                .map(InputValueDefinition::getName)
                                .collect(Collectors.toSet())))
                .filter(directive -> directive.getInputValueDefinitions().stream()
                        .map(InputValueDefinition::getType)
                        .allMatch(type -> type instanceof TypeName &&
                                DIRECTIVE_INPUT_TYPE_NAME.equals(((TypeName) type).getName())))
                .isPresent()
        );
    }

    @Test
    void add_fromTypeDefinitionRegistry_withoutIncorrectDirectiveArgs_shouldThrowException() {
        assertThrows(InvalidAuthDirectiveException.class, () ->
                AccessRuleStorage.newAccessRuleStorage()
                        .fromTypeDefinitionRegistry(loadSchema("schema_with_incorrect_directive_args.graphqls"))
                        .build());
    }

    @Test
    void add_fromTypeDefinitionRegistry_withoutIncorrectDirectiveLocations_shouldThrowException() {
        assertThrows(InvalidAuthDirectiveException.class, () ->
                AccessRuleStorage.newAccessRuleStorage()
                        .fromTypeDefinitionRegistry(loadSchema("schema_with_incorrect_directive_locations.graphqls"))
                        .build());
    }

    private TokenExpression getOneTokenEqualityExpression(String value) {
        TokenExpression tokenExpression = new TokenExpression();
        ComparisonToken comparisonToken = ComparisonToken.builder()
                .firstValue(value, ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(value, ValueType.STRING)
                .comparisonType(ComparisonType.EQUALS)
                .build();
        tokenExpression.addToken(comparisonToken);
        return tokenExpression;
    }

    private TypeDefinitionRegistry loadSchema(String fileName) throws URISyntaxException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException("Schema can't be null");
        }

        SchemaParser schemaParser = new SchemaParser();
        return schemaParser.parse(Files.lines(Paths.get(resource.toURI())).collect(Collectors.joining("\n")));
    }
}
