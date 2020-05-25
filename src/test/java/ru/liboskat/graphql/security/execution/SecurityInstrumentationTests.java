package ru.liboskat.graphql.security.execution;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.liboskat.graphql.security.storage.AccessRuleStorage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Collectors;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;
import static org.junit.jupiter.api.Assertions.*;

class SecurityInstrumentationTests {
    private static GraphQL graphQL;

    @BeforeAll
    static void setup() throws URISyntaxException, IOException {
        ClassLoader classLoader = SecurityInstrumentationTests.class.getClassLoader();
        URL resource = classLoader.getResource("schema.graphqls");
        if (resource == null) {
            throw new IllegalArgumentException("Schema can't be null");
        }

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(
                Files.lines(Paths.get(resource.toURI())).collect(Collectors.joining("\n")));

        AccessRuleStorage accessRuleStorage = AccessRuleStorage.newAccessRuleStorage()
                .fromTypeDefinitionRegistry(typeDefinitionRegistry)
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry,
                newRuntimeWiring()
                        .type("Query", builder -> builder
                                .dataFetcher("query", (env) -> new Object()))
                        .type("Mutation", builder -> builder
                                .dataFetcher("mutation", (env) -> new Object()))
                        .type("Output", builder -> builder
                                .dataFetcher("scalarField", (env) -> 1)
                                .dataFetcher("objectField", (env) -> new Object()))
                        .type("OutputObjectFieldType", builder -> builder
                                .dataFetcher("field", (env) -> "field"))
                        .build());

        graphQL = GraphQL.newGraphQL(graphQLSchema)
                .instrumentation(SecurityInstrumentation.newSecurityInstrumentation(accessRuleStorage).build())
                .build();
    }


    @Test
    void queryScalarField_withCorrectSimpleContext_shouldHaveZeroErrors() {
        queryScalarField_withCorrectContext_shouldHaveZeroErrors(SecurityContext.newSecurityContext()
                .field("schema", "schema")
                .field("scalarField", "scalarField")
                .build());
    }

    @Test
    void queryScalarField_withCorrectContextInMap_shouldHaveZeroErrors() {
        queryScalarField_withCorrectContext_shouldHaveZeroErrors(
                Collections.singletonMap("security", SecurityContext.newSecurityContext()
                        .field("schema", "schema")
                        .field("scalarField", "scalarField")
                        .build()));
    }

    @Test
    void queryScalarField_withCorrectContextInObject_shouldHaveZeroErrors() {
        queryScalarField_withCorrectContext_shouldHaveZeroErrors(new Object() {
            SecurityContext context = SecurityContext.newSecurityContext()
                    .field("schema", "schema")
                    .field("scalarField", "scalarField")
                    .build();
        });
    }

    @Test
    void queryScalarField__withContextRules_withNullContext_shouldHaveErrors() {
        ExecutionResult executionResult = graphQL.execute(
                ExecutionInput.newExecutionInput()
                        .query("query myQuery { " +
                                "query { " +
                                "scalarField " +
                                "}" +
                                "}")
                        .build());
        assertEquals(1, executionResult.getErrors().size());
    }

    @Test
    void queryObjectField_withCorrectContextAndArgument_shouldHaveZeroErrors() {
        ExecutionResult executionResult = graphQL.execute(
                ExecutionInput.newExecutionInput()
                        .context(SecurityContext.newSecurityContext()
                                .field("schema", "schema")
                                .field("outputObjectFieldType", "outputObjectFieldType")
                                .field("scalarField2", "scalarField2")
                                .build())
                        .query("query myQuery { " +
                                "query { " +
                                "objectField(argument: \"argument\") {" +
                                "field" +
                                "} " +
                                "}" +
                                "}")
                        .build());
        assertEquals(0, executionResult.getErrors().size());
    }

    @Test
    void queryObjectField_withIncorrectOutputObjectFieldTypeFieldVariable_shouldHaveErrors() {
        ExecutionResult executionResult = graphQL.execute(
                ExecutionInput.newExecutionInput()
                        .context(SecurityContext.newSecurityContext()
                                .field("schema", "schema")
                                .field("outputObjectFieldType", "outputObjectFieldType")
                                .field("scalarField2", "incorrect")
                                .build())
                        .query("query myQuery { " +
                                "query { " +
                                "objectField(argument: \"argument\") {" +
                                "field" +
                                "} " +
                                "}" +
                                "}")
                        .build());
        assertNotEquals(0, executionResult.getErrors().size());
    }

    @Test
    void queryObjectField_withIncorrectOutputObjectFieldTypeVariable_shouldHaveErrors() {
        ExecutionResult executionResult = graphQL.execute(
                ExecutionInput.newExecutionInput()
                        .context(SecurityContext.newSecurityContext()
                                .field("schema", "schema")
                                .field("outputObjectFieldType", "incorrect")
                                .field("scalarField2", "scalarField2")
                                .build())
                        .query("query myQuery { " +
                                "query { " +
                                "objectField(argument: \"argument\") {" +
                                "field" +
                                "} " +
                                "}" +
                                "}")
                        .build());
        assertNotEquals(0, executionResult.getErrors().size());
    }

    @Test
    void queryObjectField_withIncorrectSchemaVariable_shouldHaveErrors() {
        ExecutionResult executionResult = graphQL.execute(
                ExecutionInput.newExecutionInput()
                        .context(SecurityContext.newSecurityContext()
                                .field("schema", "incorrect")
                                .field("outputObjectFieldType", "outputObjectFieldType")
                                .field("scalarField2", "scalarField2")
                                .build())
                        .query("query myQuery { " +
                                "query { " +
                                "objectField(argument: \"argument\") {" +
                                "field" +
                                "} " +
                                "}" +
                                "}")
                        .build());
        assertNotEquals(0, executionResult.getErrors().size());
    }

    @Test
    void mutationScalarField_withoutInput_withCorrectContext_shouldHaveZeroErrors() {
        ExecutionResult executionResult = graphQL.execute(
                ExecutionInput.newExecutionInput()
                        .context(SecurityContext.newSecurityContext()
                                .field("schema", "schema")
                                .field("scalarField", "scalarField")
                                .build())
                        .query("mutation myMutation { " +
                                "mutation " +
                                "{ " +
                                "scalarField " +
                                "}" +
                                "}")
                        .build());
        assertEquals(0, executionResult.getErrors().size());
    }

    @Test
    void mutationScalarField_withInput_withCorrectContext_shouldHaveZeroErrors() {
        ExecutionResult executionResult = graphQL.execute(
                ExecutionInput.newExecutionInput()
                        .context(SecurityContext.newSecurityContext()
                                .field("schema", "schema")
                                .field("scalarField", "scalarField")
                                .field("input", "input")
                                .field("inputField", "inputField")
                                .field("inputObjectFieldType", "inputObjectFieldType")
                                .field("scalarField3", "scalarField3")
                                .build())
                        .query("mutation myMutation { " +
                                "mutation " +
                                "(argument: { " +
                                "objectField: { " +
                                "field: \"string\"" +
                                "}" +
                                "}) " +
                                "{ " +
                                "scalarField " +
                                "}" +
                                "}")
                        .build());
        assertEquals(0, executionResult.getErrors().size());
    }

    @Test
    void mutationScalarField_withInput_withIncorrectInputObjectFieldTypeFieldVariable_shouldHaveErrors() {
        ExecutionResult executionResult = graphQL.execute(
                ExecutionInput.newExecutionInput()
                        .context(SecurityContext.newSecurityContext()
                                .field("schema", "schema")
                                .field("scalarField", "scalarField")
                                .field("input", "input")
                                .field("inputField", "inputField")
                                .field("inputObjectFieldType", "inputObjectFieldType")
                                .field("scalarField3", "incorrect")
                                .build())
                        .query("mutation myMutation { " +
                                "mutation " +
                                "(argument: { " +
                                "objectField: { " +
                                "field: \"string\"" +
                                "}" +
                                "}) " +
                                "{ " +
                                "scalarField " +
                                "}" +
                                "}")
                        .build());
        assertNotEquals(0, executionResult.getErrors().size());
    }

    @Test
    void mutationScalarField_withInput_withIncorrectInputObjectFieldTypeVariable_shouldHaveErrors() {
        ExecutionResult executionResult = graphQL.execute(
                ExecutionInput.newExecutionInput()
                        .context(SecurityContext.newSecurityContext()
                                .field("schema", "schema")
                                .field("scalarField", "scalarField")
                                .field("input", "input")
                                .field("inputField", "inputField")
                                .field("inputObjectFieldType", "incorrect")
                                .field("scalarField3", "scalarField3")
                                .build())
                        .query("mutation myMutation { " +
                                "mutation " +
                                "(argument: { " +
                                "objectField: { " +
                                "field: \"string\"" +
                                "}" +
                                "}) " +
                                "{ " +
                                "scalarField " +
                                "}" +
                                "}")
                        .build());
        assertNotEquals(0, executionResult.getErrors().size());
    }

    @Test
    void mutationScalarField_withInput_withIncorrectInputFieldVariable_shouldHaveErrors() {
        ExecutionResult executionResult = graphQL.execute(
                ExecutionInput.newExecutionInput()
                        .context(SecurityContext.newSecurityContext()
                                .field("schema", "schema")
                                .field("scalarField", "scalarField")
                                .field("input", "input")
                                .field("inputField", "incorrect")
                                .field("inputObjectFieldType", "inputObjectFieldType")
                                .field("scalarField3", "scalarField3")
                                .build())
                        .query("mutation myMutation { " +
                                "mutation " +
                                "(argument: { " +
                                "objectField: { " +
                                "field: \"string\"" +
                                "}" +
                                "}) " +
                                "{ " +
                                "scalarField " +
                                "}" +
                                "}")
                        .build());
        assertNotEquals(0, executionResult.getErrors().size());
    }

    @Test
    void mutationScalarField_withInput_withIncorrectInputVariable_shouldHaveErrors() {
        ExecutionResult executionResult = graphQL.execute(
                ExecutionInput.newExecutionInput()
                        .context(SecurityContext.newSecurityContext()
                                .field("schema", "schema")
                                .field("scalarField", "scalarField")
                                .field("input", "incorrect")
                                .field("inputField", "inputField")
                                .field("inputObjectFieldType", "inputObjectFieldType")
                                .field("scalarField3", "scalarField3")
                                .build())
                        .query("mutation myMutation { " +
                                "mutation " +
                                "(argument: { " +
                                "objectField: { " +
                                "field: \"string\"" +
                                "}" +
                                "}) " +
                                "{ " +
                                "scalarField " +
                                "}" +
                                "}")
                        .build());
        assertNotEquals(0, executionResult.getErrors().size());
    }

    @Test
    void mutationScalarField_withInput_withIncorrectOutputFieldVariable_shouldHaveErrors() {
        ExecutionResult executionResult = graphQL.execute(
                ExecutionInput.newExecutionInput()
                        .context(SecurityContext.newSecurityContext()
                                .field("schema", "schema")
                                .field("scalarField", "incorrect")
                                .field("input", "input")
                                .field("inputField", "inputField")
                                .field("inputObjectFieldType", "inputObjectFieldType")
                                .field("scalarField3", "scalarField3")
                                .build())
                        .query("mutation myMutation { " +
                                "mutation " +
                                "(argument: { " +
                                "objectField: { " +
                                "field: \"string\"" +
                                "}" +
                                "}) " +
                                "{ " +
                                "scalarField " +
                                "}" +
                                "}")
                        .build());
        assertNotEquals(0, executionResult.getErrors().size());
    }

    @Test
    void mutationScalarField_withInput_withIncorrectSchemaVariable_shouldHaveErrors() {
        ExecutionResult executionResult = graphQL.execute(
                ExecutionInput.newExecutionInput()
                        .context(SecurityContext.newSecurityContext()
                                .field("schema", "incorrect")
                                .field("scalarField", "scalarField")
                                .field("input", "input")
                                .field("inputField", "inputField")
                                .field("inputObjectFieldType", "inputObjectFieldType")
                                .field("scalarField3", "scalarField3")
                                .build())
                        .query("mutation myMutation { " +
                                "mutation " +
                                "(argument: { " +
                                "objectField: { " +
                                "field: \"string\"" +
                                "}" +
                                "}) " +
                                "{ " +
                                "scalarField " +
                                "}" +
                                "}")
                        .build());
        assertNotEquals(0, executionResult.getErrors().size());
    }

    @Test
    void build_WithNullAccessRuleStorage_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> SecurityInstrumentation.newSecurityInstrumentation().build());
    }

    private void queryScalarField_withCorrectContext_shouldHaveZeroErrors(Object context) {
        ExecutionResult executionResult = graphQL.execute(
                ExecutionInput.newExecutionInput()
                        .context(context)
                        .query("query myQuery { " +
                                "query { " +
                                "scalarField " +
                                "}" +
                                "}")
                        .build());
        assertEquals(0, executionResult.getErrors().size());
    }
}
