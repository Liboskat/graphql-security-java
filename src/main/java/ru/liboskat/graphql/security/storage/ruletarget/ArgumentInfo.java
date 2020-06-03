package ru.liboskat.graphql.security.storage.ruletarget;

import java.util.Objects;

import static ru.liboskat.graphql.security.utils.StringUtils.isNullOrEmpty;

/**
 * Используется в качестве ключа в {@link ru.liboskat.graphql.security.storage.AccessRuleStorage} и
 * передается в {@link ru.liboskat.graphql.security.exceptions.AuthException} в случае ошибок при выполнении запроса,
 * хранит информацию об аргументе поля
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

    /**
     * @return название родительского типа
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * @return название поля
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * @return название аргумента
     */
    public String getArgumentName() {
        return argumentName;
    }

    /**
     * Создает новый {@link ArgumentInfo} на основе переданных параметров
     *
     * @param typeName     название родительского типа
     * @param fieldName    название поля
     * @param argumentName название аргумента поля
     * @return новый {@link ArgumentInfo} на основе переданных параметров
     * @throws IllegalArgumentException, если название типа или название поля или название аргумента
     *                                   являются null или пустыми
     */
    public static ArgumentInfo newArgumentInfo(String typeName, String fieldName, String argumentName) {
        if (isNullOrEmpty(typeName) || isNullOrEmpty(fieldName) || isNullOrEmpty(argumentName)) {
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