package ru.liboskat.graphql.security.storage.ruletarget;

import java.util.Objects;

import static ru.liboskat.graphql.security.utils.StringUtils.isNullOrEmpty;

public class FieldInfo implements RuleTargetInfo {
    private final String typeName;
    private final String fieldName;

    private FieldInfo(String typeName, String fieldName) {
        this.typeName = typeName;
        this.fieldName = fieldName;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public static FieldInfo newFieldInfo(String typeName, String fieldName) {
        if (isNullOrEmpty(typeName) || isNullOrEmpty(fieldName) ) {
            throw new IllegalArgumentException("TypeName and fieldName can't be null or empty");
        }
        return new FieldInfo(typeName, fieldName);
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