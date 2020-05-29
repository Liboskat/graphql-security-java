package ru.liboskat.graphql.security.storage.ruletarget;

import java.util.Objects;

import static ru.liboskat.graphql.security.utils.StringUtils.isNullOrEmpty;

/**
 * Class with information about object
 */
public class ObjectInfo implements RuleTargetInfo {
    private final String name;

    private ObjectInfo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * @param name name of type
     * @throws IllegalArgumentException if name is null or empty
     */
    public static ObjectInfo newObjectInfo(String name) {
        if (isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Object name can't be null or empty");
        }
        return new ObjectInfo(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectInfo that = (ObjectInfo) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Object{" +
                "name='" + name + '\'' +
                '}';
    }
}
