package ru.liboskat.graphql.security.storage.ruletarget;

import java.util.Objects;

import static ru.liboskat.graphql.security.utils.StringUtils.isNullOrEmpty;

/**
 * Используется в качестве ключа в {@link ru.liboskat.graphql.security.storage.AccessRuleStorage} и
 * передается в {@link ru.liboskat.graphql.security.exceptions.AuthException}в случае ошибок при выполнении запроса,
 * хранит информацию о поле входного объекта
 */
public class InputFieldInfo implements RuleTargetInfo {
    private final String inputTypeName;
    private final String fieldName;

    private InputFieldInfo(String inputTypeName, String fieldName) {
        this.inputTypeName = inputTypeName;
        this.fieldName = fieldName;
    }

    /**
     * @return название родительского типа
     */
    public String getInputTypeName() {
        return inputTypeName;
    }

    /**
     * @return название поля
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Создает новый {@link InputFieldInfo} на основе переданных параметров
     *
     * @param inputTypeName название родительского типа
     * @param fieldName     название поля
     * @return новый {@link InputFieldInfo} на основе переданных параметров
     * @throws IllegalArgumentException, если название типа или название поля являются null или пустыми
     */
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
