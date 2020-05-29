package ru.liboskat.graphql.security.storage.ruletarget;

import java.util.Objects;

import static ru.liboskat.graphql.security.utils.StringUtils.isNullOrEmpty;

/**
 * Class with information about argument
 */
public class ArgumentInfo implements RuleTargetInfo {
    private final String typeName;
    private final String fieldName;
    private final String argumentName;

    private ArgumentInfo(String typeName, String fieldName, String argumentName) {
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

    /**
     * @param typeName name of parent type
     * @param fieldName field name
     * @param argumentName argument name
     * @throws IllegalArgumentException if one or more of typeName, fieldName, argumentName is null or empty
     */
    public static ArgumentInfo newArgumentInfo(String typeName, String fieldName, String argumentName) {
        if (isNullOrEmpty(typeName) || isNullOrEmpty(fieldName)  || isNullOrEmpty(argumentName)) {
            throw new IllegalArgumentException("TypeName, fieldName and argumentName can't be null or empty");
        }
        return new ArgumentInfo(typeName, fieldName, argumentName);
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