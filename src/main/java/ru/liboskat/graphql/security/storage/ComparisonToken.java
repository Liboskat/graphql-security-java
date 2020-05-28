package ru.liboskat.graphql.security.storage;

import java.util.Objects;

/**
 * An implementation of {@link Token} that is used to store information about comparison
 */
public class ComparisonToken implements Token {
    private final Object firstValue;
    private final Object secondValue;
    private final ValueType firstValueType;
    private final ValueType secondValueType;
    private final ComparisonType comparisonType;

    private ComparisonToken(Object firstValue, ValueType firstValueType,
                            Object secondValue, ValueType secondValueType, ComparisonType comparisonType) {
        this.firstValue = firstValue;
        this.firstValueType = firstValueType;
        this.secondValue = secondValue;
        this.secondValueType = secondValueType;
        this.comparisonType = comparisonType;
    }

    public Object getFirstValue() {
        return firstValue;
    }

    public Object getSecondValue() {
        return secondValue;
    }

    public ValueType getFirstValueType() {
        return firstValueType;
    }

    public ValueType getSecondValueType() {
        return secondValueType;
    }

    public ComparisonType getComparisonType() {
        return comparisonType;
    }

    /**
     * Compares ComparisonToken objects correctly
     * Let be 'a' - first object and 'b' - second object, '1' - first value and type, '2' - second value and type
     * For example a2 is first object second value and type
     * '<' is opposite to '>' and '<=' is opposite to '=>'
     * Then objects is equal if:
     * 1. if objects comparison type = '=':
     *        a1=b1 & a2=b2 | a1=b2 & a2=b1
     * 2. if first object type != '=' and second object type is same:
     *        a1=b1 & a2=b2
     * 2. if first object type != '=' and second object type is opposite:
     *        a1=b2 & a2=b1
     * @param o other object
     * @return result of comparison
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComparisonToken that = (ComparisonToken) o;
        return comparisonType == that.comparisonType && comparisonType == ComparisonType.EQUALS &&
                equalsWithEquality(that) ||
                comparisonType == that.comparisonType && comparisonType != ComparisonType.EQUALS &&
                        equalsWithSameInequality(that) ||
                comparisonType != that.comparisonType && isComparisonTypeOppositeTo(that.comparisonType) &&
                        equalsWithOppositeInequality(that);

    }

    private boolean equalsWithEquality(ComparisonToken that) {
        return isValuesEqual(firstValue, firstValueType, that.firstValue, that.firstValueType) &&
                isValuesEqual(secondValue, secondValueType, that.secondValue, that.secondValueType) ||
                isValuesEqual(firstValue, firstValueType, that.secondValue, that.secondValueType) &&
                        isValuesEqual(secondValue, secondValueType, that.firstValue, that.firstValueType);
    }

    private boolean equalsWithSameInequality(ComparisonToken that) {
        return isValuesEqual(firstValue, firstValueType, that.firstValue, that.firstValueType) &&
                isValuesEqual(secondValue, secondValueType, that.secondValue, that.secondValueType);
    }

    private boolean isComparisonTypeOppositeTo(ComparisonType thatComparisonType) {
        return comparisonType == ComparisonType.LT && thatComparisonType == ComparisonType.GT ||
                comparisonType == ComparisonType.LTE && thatComparisonType == ComparisonType.GTE ||
                comparisonType == ComparisonType.GT && thatComparisonType == ComparisonType.LT ||
                comparisonType == ComparisonType.GTE && thatComparisonType == ComparisonType.LTE;
    }

    private boolean equalsWithOppositeInequality(ComparisonToken that) {
        return isValuesEqual(firstValue, firstValueType, that.secondValue, that.secondValueType) &&
                isValuesEqual(secondValue, secondValueType, that.firstValue, that.firstValueType);
    }

    private boolean isValuesEqual(Object firstObjectValue, ValueType firstObjectValueType,
                                  Object secondObjectValue, ValueType secondObjectValueType) {
        return Objects.equals(firstObjectValue, secondObjectValue) && firstObjectValueType == secondObjectValueType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstValue, secondValue, firstValueType, secondValueType, comparisonType) / 2 +
                Objects.hash(secondValue, firstValue, secondValueType, firstValueType, comparisonType) / 2;
    }

    /**
     * @return {@link Builder} for this class
     */
    public static ComparisonToken.Builder builder() {
        return new Builder();
    }

    /**
     * Type of comparison operand value
     */
    public enum ValueType {
        GRAPHQL_CONTEXT_FIELD_NAME,
        GRAPHQL_ARGUMENT_NAME,
        STRING,
        NULL,
        BOOLEAN,
        INTEGER,
        LONG,
        DOUBLE,
        ZONED_DATE_TIME,
        LOCAL_DATE_TIME,
        LOCAL_DATE,
        LOCAL_TIME
    }

    /**
     * Type of operand comparison
     */
    public enum ComparisonType {
        EQUALS("="),
        LT("<"),
        GT(">"),
        LTE("<="),
        GTE(">=");

        private final String stringRepresentation;

        ComparisonType(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }

        public String getStringRepresentation() {
            return stringRepresentation;
        }
    }

    /**
     * Used to indicate that value is null
     */
    public enum NullValue {
        INSTANCE
    }

    /**
     * Class that is used to construct new {@link ComparisonToken}
     */
    public static class Builder {
        private Object firstValue;
        private Object secondValue;
        private ValueType firstValueType;
        private ValueType secondValueType;
        private ComparisonType comparisonType;

        /**
         * Sets first operand value and type
         * @param value first operand value
         * @param valueType first operand type
         * @return this builder
         * @throws IllegalArgumentException if value or type is null
         */
        public Builder firstValue(Object value, ValueType valueType) {
            throwIfNullValueOrType(value);
            throwIfNullValueOrType(valueType);
            this.firstValue = value;
            this.firstValueType = valueType;
            return this;
        }

        /**
         * Sets operand first value
         * @param value first operand value
         * @return this builder
         * @throws IllegalArgumentException if value is null
         */
        public Builder firstValue(Object value) {
            throwIfNullValueOrType(value);
            this.firstValue = value;
            return this;
        }

        /**
         * Sets operand first type
         * @param valueType first operand type
         * @return this builder
         * @throws IllegalArgumentException if type is null
         */
        public Builder firstValueType(ValueType valueType) {
            throwIfNullValueOrType(valueType);
            this.firstValueType = valueType;
            return this;
        }

        /**
         * Sets second operand value and type
         * @param value second operand value
         * @param valueType second operand type
         * @return this builder
         * @throws IllegalArgumentException if value or type is null
         */
        public Builder secondValue(Object value, ValueType valueType) {
            throwIfNullValueOrType(value);
            throwIfNullValueOrType(valueType);
            this.secondValue = value;
            this.secondValueType = valueType;
            return this;
        }

        /**
         * Sets second operand value
         * @param value second operand value
         * @return this builder
         * @throws IllegalArgumentException if value is null
         */
        public Builder secondValue(Object value) {
            throwIfNullValueOrType(value);
            this.secondValue = value;
            return this;
        }

        /**
         * Sets second operand value and type
         * @param valueType second operand type
         * @return this builder
         * @throws IllegalArgumentException if type is null
         */
        public Builder secondValueType(ValueType valueType) {
            throwIfNullValueOrType(valueType);
            this.secondValueType = valueType;
            return this;
        }

        /**
         * Sets comparison type
         * @param comparisonType comparison type
         * @return this builder
         * @throws IllegalArgumentException if type is null
         */
        public Builder comparisonType(ComparisonType comparisonType) {
            throwIfNullValueOrType(comparisonType);
            this.comparisonType = comparisonType;
            return this;
        }

        private void throwIfNullValueOrType(Object nullableObject) {
            if (nullableObject == null) {
                throw new IllegalArgumentException("Value or type can't be null");
            }
        }

        /**
         * @return constructed {@link ComparisonToken}
         * @throws IllegalArgumentException if some values or types is null or types can't be compared
         * or type can't be compared using comparison type
         */
        public ComparisonToken build() {
            if (firstValue == null || firstValueType == null || secondValue == null || secondValueType == null) {
                throw new IllegalArgumentException("Values and types can't be null");
            }
            if (comparisonType == null) {
                comparisonType = ComparisonType.EQUALS;
            }
            if (comparisonType != ComparisonType.EQUALS && (firstValueType == ValueType.STRING ||
                    firstValueType == ValueType.BOOLEAN || firstValueType == ValueType.NULL ||
                    secondValueType == ValueType.STRING || secondValueType == ValueType.BOOLEAN ||
                    secondValueType == ValueType.NULL)) {
                throw new IllegalArgumentException(String.format("Operation %s is illegal with type %s",
                        comparisonType.getStringRepresentation(), firstValueType));
            }
            if (firstValueType != secondValueType &&
                    firstValueType != ValueType.GRAPHQL_ARGUMENT_NAME &&
                    firstValueType != ValueType.GRAPHQL_CONTEXT_FIELD_NAME &&
                    secondValueType != ValueType.GRAPHQL_ARGUMENT_NAME &&
                    secondValueType != ValueType.GRAPHQL_CONTEXT_FIELD_NAME) {
                throw new IllegalArgumentException(String.format("Can't compare values with types: %s, %s",
                        firstValueType, secondValueType));
            }
            return new ComparisonToken(firstValue, firstValueType, secondValue, secondValueType, comparisonType);
        }
    }

    @Override
    public String toString() {
        return "(" + firstValueType + " " + firstValue +
                comparisonType.getStringRepresentation() + secondValueType + " " + secondValue +
                ")";
    }
}
