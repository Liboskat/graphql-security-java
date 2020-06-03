package ru.liboskat.graphql.security.storage.ruletarget;

import java.util.Objects;

import static ru.liboskat.graphql.security.utils.StringUtils.isNullOrEmpty;

/**
 * Используется в качестве ключа в {@link ru.liboskat.graphql.security.storage.AccessRuleStorage} и
 * передается в {@link ru.liboskat.graphql.security.exceptions.AuthException}в случае ошибок при выполнении запроса,
 * хранит информацию о входном объекте
 */
public class InputObjectInfo implements RuleTargetInfo {
    private final String name;

    private InputObjectInfo(String name) {
        this.name = name;
    }

    /**
     * @return название типа
     */
    public String getName() {
        return name;
    }

    /**
     * Создает новый {@link InputObjectInfo} на основе переданных параметров
     *
     * @param name название типа
     * @return новый {@link InputObjectInfo} на основе переданных параметров
     * @throws IllegalArgumentException, если название типа является null или пустыми
     */
    public static InputObjectInfo newInputObjectInfo(String name) {
        if (isNullOrEmpty(name)) {
            throw new IllegalArgumentException("InputObject name can't be null or empty");
        }
        return new InputObjectInfo(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputObjectInfo that = (InputObjectInfo) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Input{" +
                "name='" + name + '\'' +
                '}';
    }
}
