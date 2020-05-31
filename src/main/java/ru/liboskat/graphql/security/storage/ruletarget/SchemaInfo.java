package ru.liboskat.graphql.security.storage.ruletarget;

/**
 * todo javadoc
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
