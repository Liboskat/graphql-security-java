package ru.liboskat.graphql.security.storage.ruletarget;

/**
 * Передается в {@link ru.liboskat.graphql.security.exceptions.AuthException} в случае ошибок при выполнении запроса
 * для обозначения того, что не разрешен доступ к схеме
 */
public class SchemaInfo implements RuleTargetInfo {
    private SchemaInfo() {
    }

    public static SchemaInfo newSchemaInfo() {
        return new SchemaInfo();
    }

    @Override
    public String toString() {
        return "Schema";
    }
}
