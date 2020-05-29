package ru.liboskat.graphql.security.storage.ruletarget;

/**
 * Class that shows that target is schema
 */
public class SchemaInfo implements RuleTargetInfo {
    private SchemaInfo() {}

    public static SchemaInfo newSchemaInfo() {
        return new SchemaInfo();
    }

    @Override
    public String toString() {
        return "Schema";
    }
}
