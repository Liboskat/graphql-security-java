package ru.liboskat.graphql.security.storage.ruletarget;

import java.util.Objects;

import static ru.liboskat.graphql.security.utils.StringUtils.isNullOrEmpty;

public class InputFieldInfo implements RuleTargetInfo {
    private final String inputTypeName;
    private final String fieldName;

    private InputFieldInfo(String inputTypeName, String fieldName) {
        this.inputTypeName = inputTypeName;
        this.fieldName = fieldName;
    }

    public String getInputTypeName() {
        return inputTypeName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public static InputFieldInfo newInputFieldInfo(String inputTypeName, String fieldName) {
        if (isNullOrEmpty(inputTypeName) || isNullOrEmpty(fieldName)) {
            throw new IllegalArgumentException("InputTypeName and fieldName can't be null or empty");
        }
        return new InputFieldInfo(inputTypeName, fieldName);
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
