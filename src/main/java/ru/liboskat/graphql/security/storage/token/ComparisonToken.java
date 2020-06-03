package ru.liboskat.graphql.security.storage.token;

import ru.liboskat.graphql.security.utils.TemporalToZonedDateTimeConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.Objects;

/**
 * Хранит информацию о сравнении операндов
 */
public class ComparisonToken implements Token {
    /**
     * Первый операнд
     */
    private final Object firstValue;
    /**
     * Второй операнд
     */
    private final Object secondValue;
    /**
     * Тип первого операнда
     */
    private final ValueType firstValueType;
    /**
     * Тип второго операнда
     */
    private final ValueType secondValueType;
    /**
     * Тип сравнения
     */
    private final ComparisonType comparisonType;

    private ComparisonToken(Object firstValue, ValueType firstValueType,
                            Object secondValue, ValueType secondValueType, ComparisonType comparisonType) {
        this.firstValue = firstValue;
        this.firstValueType = firstValueType;
        this.secondValue = secondValue;
        this.secondValueType = secondValueType;
        this.comparisonType = comparisonType;
    }

    /**
     * @return первый операнд
     */
    public Object getFirstValue() {
        return firstValue;
    }

    /**
     * @return второй операнд
     */
    public Object getSecondValue() {
        return secondValue;
    }

    /**
     * @return тип первого операнда
     */
    public ValueType getFirstValueType() {
        return firstValueType;
    }

    /**
     * @return тип второго операнда
     */
    public ValueType getSecondValueType() {
        return secondValueType;
    }

    /**
     * @return тип сравнения
     */
    public ComparisonType getComparisonType() {
        return comparisonType;
    }

    /**
     * <p>
     * Сравнивает объекты {@link ComparisonToken}
     * Существуют специальные правила для сравнения.
     * </p>
     * <p>
     * Пусть 'a' - первый объект and 'b' - второй объект,
     * '1' - значение и тип первого операнда, '2' - значение и тип второго операнда.
     * Тип сравнения '<' является противоположным для '>' и '<=' противоположен '=>'.
     * </p>
     * <p>
     * Тогда объекты являются равными, если:
     * <ol>
     *     <li>
     *         если тип сравнения = '=' и соблюдается:
     *         a1=b1 & a2=b2 | a1=b2 & a2=b1
     *     </li>
     *     <li>
     *         если тип сравнения = '=' и соблюдается:
     *         a1=b1 & a2=b2 | a1=b2 & a2=b1
     *      </li>
     *      <li>
     *          если тип сравнения = '=' и соблюдается:
     *          a1=b1 & a2=b2 | a1=b2 & a2=b1
     *      </li>
     * </ol>
     * </p>
     * В любом другом случае объекты не равны
     *
     * @param o объект для сравнения с текущим
     * @return результат сравнения объектов {@link ComparisonToken}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComparisonToken that = (ComparisonToken) o;
        return  // проверка, если тип сравнения у объектов - '='
                comparisonType == that.comparisonType && comparisonType == ComparisonType.EQUALS &&
                        equalsWithEquality(that) ||
                        // проверка, если тип сравнения у объектов - одинаковое неравенство
                        comparisonType == that.comparisonType && comparisonType != ComparisonType.EQUALS &&
                                equalsWithSameInequality(that) ||
                        // проверка, если тип сравнения у объектов - противоположное неравенство
                        comparisonType != that.comparisonType && isComparisonTypeOppositeTo(that.comparisonType) &&
                                equalsWithOppositeInequality(that);
    }

    /**
     * Возвращает результат сравнения для объектов с типом сравнения - '='
     *
     * @param that объект для сравнения
     * @return true, если операнды одинаковые вне зависимости от очередности
     */
    private boolean equalsWithEquality(ComparisonToken that) {
        return isValuesEqual(firstValue, firstValueType, that.firstValue, that.firstValueType) &&
                isValuesEqual(secondValue, secondValueType, that.secondValue, that.secondValueType) ||
                isValuesEqual(firstValue, firstValueType, that.secondValue, that.secondValueType) &&
                        isValuesEqual(secondValue, secondValueType, that.firstValue, that.firstValueType);
    }

    /**
     * Возвращает результат сравнения для объектов с одинаковым типом сравнения - неравенство
     *
     * @param that объект для сравнения
     * @return true, если соответсвующие операнды одинаковые (первый с первым, второй со вторым), иначе false
     */
    private boolean equalsWithSameInequality(ComparisonToken that) {
        return isValuesEqual(firstValue, firstValueType, that.firstValue, that.firstValueType) &&
                isValuesEqual(secondValue, secondValueType, that.secondValue, that.secondValueType);
    }

    /**
     * Проверяет, являются ли типы сравнения у объектов - противоположными неравенствами
     *
     * @param thatComparisonType тип сравнения другого объекта
     * @return true, если тип сравнения - противоположное неравенство
     * ('<' противоположен '>', '<=' противоположен '>='),
     * иначе false
     */
    private boolean isComparisonTypeOppositeTo(ComparisonType thatComparisonType) {
        return comparisonType == ComparisonType.LT && thatComparisonType == ComparisonType.GT ||
                comparisonType == ComparisonType.LTE && thatComparisonType == ComparisonType.GTE ||
                comparisonType == ComparisonType.GT && thatComparisonType == ComparisonType.LT ||
                comparisonType == ComparisonType.GTE && thatComparisonType == ComparisonType.LTE;
    }

    /**
     * Возвращает результат сравнения для объектов с противоположными типами сравнения - неравенство
     *
     * @param that объект для сравнения
     * @return true, если операнды в противоположном порядке одинаковые (первый со вторым, второй с первым),
     * иначе false
     */
    private boolean equalsWithOppositeInequality(ComparisonToken that) {
        return isValuesEqual(firstValue, firstValueType, that.secondValue, that.secondValueType) &&
                isValuesEqual(secondValue, secondValueType, that.firstValue, that.firstValueType);
    }

    /**
     * Проверяет, являются ли тип и значение одного операнда соответсвующе равны типу и значению второго
     *
     * @param firstObjectValue      значение первого операнда
     * @param firstObjectValueType  тип первого операнда
     * @param secondObjectValue     значение второго операнда
     * @param secondObjectValueType тип второго операнда
     * @return true, если тип и значение одного операнда соответсвующе равны типу и значению второго, иначе false
     */
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
     * @return {@link Builder} для этого объекта
     */
    public static ComparisonToken.Builder builder() {
        return new Builder();
    }

    /**
     * Тип операнда
     */
    public enum ValueType {
        GRAPHQL_CONTEXT_FIELD_NAME,
        GRAPHQL_ARGUMENT_NAME,
        STRING,
        NULL,
        BOOLEAN,
        INTEGER,
        REAL,
        ZONED_DATE_TIME,
        LOCAL_DATE_TIME,
        LOCAL_DATE,
        LOCAL_TIME
    }

    /**
     * Тип сравнения
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
     * Объект, используемый как значение для операнда с типом {@link ValueType#NULL}
     */
    public enum NullValue {
        INSTANCE
    }

    /**
     * Класс, используемый для построения нового {@link ComparisonToken}
     */
    public static class Builder {
        private Object firstValue;
        private Object secondValue;
        private ValueType firstValueType;
        private ValueType secondValueType;
        private ComparisonType comparisonType;

        /**
         * Устанавливает тип и значение первого операнда
         *
         * @param value     значение первого операнда
         * @param valueType тип первого операнда
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException, если значение или тип операнда являются null
         */
        public Builder firstValue(Object value, ValueType valueType) {
            throwIfNullValue(value);
            throwIfNullValueType(valueType);
            this.firstValue = value;
            this.firstValueType = valueType;
            return this;
        }

        /**
         * Устанавливает значение первого операнда
         *
         * @param value значение первого операнда
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException, если значение операнда является null
         */
        public Builder firstValue(Object value) {
            throwIfNullValue(value);
            this.firstValue = value;
            return this;
        }

        /**
         * Устанавливает тип первого операнда
         *
         * @param valueType тип первого операнда
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException, если тип операнда является null
         */
        public Builder firstValueType(ValueType valueType) {
            throwIfNullValueType(valueType);
            this.firstValueType = valueType;
            return this;
        }

        /**
         * Устанавливает тип и значение второго операнда
         *
         * @param value     значение второго операнда
         * @param valueType тип второго операнда
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException, если значение или тип операнда являются null
         */
        public Builder secondValue(Object value, ValueType valueType) {
            throwIfNullValue(value);
            throwIfNullValueType(valueType);
            this.secondValue = value;
            this.secondValueType = valueType;
            return this;
        }

        /**
         * Устанавливает значение второго операнда
         *
         * @param value значение второго операнда
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException, если значение операнда является null
         */
        public Builder secondValue(Object value) {
            throwIfNullValue(value);
            this.secondValue = value;
            return this;
        }

        /**
         * Устанавливает тип второго операнда
         *
         * @param valueType тип второго операнда
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException, если тип операнда является null
         */
        public Builder secondValueType(ValueType valueType) {
            throwIfNullValueType(valueType);
            this.secondValueType = valueType;
            return this;
        }

        /**
         * Устанавливает тип сравнения
         *
         * @param comparisonType тип сравнения
         * @return текущий {@link Builder}
         * @throws IllegalArgumentException, если тип сравнения является null
         */
        public Builder comparisonType(ComparisonType comparisonType) {
            throwIfNullComparisonType(comparisonType);
            this.comparisonType = comparisonType;
            return this;
        }

        /**
         * Выбрасывает исключение, если переданное значение является null
         *
         * @param nullableObject значение операнда
         * @throws IllegalArgumentException, если значение операнда - null
         */
        private void throwIfNullValue(Object nullableObject) {
            if (nullableObject == null) {
                throw new IllegalArgumentException("Value can't be null");
            }
        }

        /**
         * Выбрасывает исключение, если переданный тип операнда является null
         *
         * @param nullableType тип операнда
         * @throws IllegalArgumentException, если тип операнда - null
         */
        private void throwIfNullValueType(ValueType nullableType) {
            if (nullableType == null) {
                throw new IllegalArgumentException("ValueType can't be null");
            }
        }

        /**
         * Выбрасывает исключение, если переданный тип сравнения является null
         *
         * @param nullableType тип сравнения
         * @throws IllegalArgumentException, если тип сравнения - null
         */
        private void throwIfNullComparisonType(ComparisonType nullableType) {
            if (nullableType == null) {
                throw new IllegalArgumentException("ComparisonType can't be null");
            }
        }

        /**
         * @return новый {@link ComparisonToken},
         * созданный с переданными значениями и типами операндов и типом сравнения
         * @throws IllegalArgumentException, если не переданы значения и типы операндов, тип сравнения или
         *                                   тип сравнения не разрешен с одним из переданных типов операнда или
         *                                   операнды с переданными типами нельзя сравнивать между собой
         */
        public ComparisonToken build() {
            //проверка значений и типов операндов, типа сравнения на null
            throwIfNullValue(firstValue);
            throwIfNullValue(secondValue);
            throwIfNullValueType(firstValueType);
            throwIfNullValueType(secondValueType);
            throwIfNullComparisonType(comparisonType);

            //проверка корректности соответствия типа сравнения типам операндов
            checkIsSuitableComparisonTypeToValueTypes();
            //проверка того, являются ли типы операндов сравнимыми между собой
            checkNotComparableTypes();

            //трансформация значений операндов в корректный для хранения тип
            firstValue = transformToCorrectJavaType(firstValue, firstValueType);
            secondValue = transformToCorrectJavaType(secondValue, secondValueType);

            return new ComparisonToken(firstValue, firstValueType, secondValue, secondValueType, comparisonType);
        }

        /**
         * Проверка корректности соответствия типа сравнения типам операндов
         *
         * @throws IllegalArgumentException, если тип одного из операндов
         *                                   {@link ValueType#STRING}, {@link ValueType#BOOLEAN} или {@link ValueType#NULL}
         *                                   и тип сравнения не '='
         */
        private void checkIsSuitableComparisonTypeToValueTypes() {
            if (comparisonType != ComparisonType.EQUALS && (firstValueType == ValueType.STRING ||
                    firstValueType == ValueType.BOOLEAN || firstValueType == ValueType.NULL ||
                    secondValueType == ValueType.STRING || secondValueType == ValueType.BOOLEAN ||
                    secondValueType == ValueType.NULL)) {
                throw new IllegalArgumentException(String.format("Operation %s is illegal with type %s",
                        comparisonType.getStringRepresentation(), firstValueType));
            }
        }

        /**
         * Проверка того, являются ли типы операндов сравнимыми между собой
         *
         * @throws IllegalArgumentException, если ни один из типов операндов не является
         *                                   названием аргумента {@link ValueType#GRAPHQL_ARGUMENT_NAME} или
         *                                   названием переменной контекста {@link ValueType#GRAPHQL_CONTEXT_FIELD_NAME}
         */
        private void checkNotComparableTypes() {
            if (firstValueType != secondValueType &&
                    firstValueType != ValueType.GRAPHQL_ARGUMENT_NAME &&
                    firstValueType != ValueType.GRAPHQL_CONTEXT_FIELD_NAME &&
                    secondValueType != ValueType.GRAPHQL_ARGUMENT_NAME &&
                    secondValueType != ValueType.GRAPHQL_CONTEXT_FIELD_NAME) {
                throw new IllegalArgumentException(String.format("Can't compare values with types: %s, %s",
                        firstValueType, secondValueType));
            }
        }

        /**
         * Преобразует значение операнда в корректный для хранения вид
         *
         * @param value значение операнда
         * @param type  тип операнда
         * @return если тип {@link ValueType#INTEGER} - значение в виде {@link Long},
         * если тип {@link ValueType#REAL} - значение в виде {@link Double},
         * если значение имеет тип {@link LocalDate} или {@link LocalTime} или {@link LocalDateTime} -
         * значение в виде {@link java.time.ZonedDateTime},
         * иначе - исходное значение
         * @throws IllegalArgumentException, если не удается преобразование объекта в корректный вид
         */
        private Object transformToCorrectJavaType(Object value, ValueType type) {
            if (type == ValueType.INTEGER && value instanceof Number) {
                value = ((Number) value).longValue();
            }
            if (type == ValueType.REAL && value instanceof Number) {
                value = ((Number) value).doubleValue();
            }
            if (value instanceof LocalDateTime || value instanceof LocalDate || value instanceof LocalTime) {
                value = TemporalToZonedDateTimeConverter.convert((Temporal) value);
            }

            return value;
        }
    }

    @Override
    public String toString() {
        return "(" + firstValueType + " " + firstValue +
                comparisonType.getStringRepresentation() + secondValueType + " " + secondValue +
                ")";
    }
}
