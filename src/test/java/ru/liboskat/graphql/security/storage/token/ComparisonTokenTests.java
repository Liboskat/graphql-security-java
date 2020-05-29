package ru.liboskat.graphql.security.storage.token;

import org.junit.jupiter.api.Test;
import ru.liboskat.graphql.security.storage.token.ComparisonToken.ComparisonType;
import ru.liboskat.graphql.security.storage.token.ComparisonToken.NullValue;
import ru.liboskat.graphql.security.storage.token.ComparisonToken.ValueType;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonTokenTests {
    @Test
    void build_withCorrectArguments_shouldHaveValues() {
        ComparisonToken comparisonToken = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.GT)
                .build();
        assertAll(() -> assertEquals(comparisonToken.getFirstValue(),
                LocalDate.parse("2018-10-10").atStartOfDay(ZoneId.systemDefault())),
                () -> assertEquals(comparisonToken.getFirstValueType(), ValueType.LOCAL_DATE),
                () -> assertEquals(comparisonToken.getSecondValue(), "value"),
                () -> assertEquals(comparisonToken.getSecondValueType(), ValueType.GRAPHQL_CONTEXT_FIELD_NAME),
                () -> assertEquals(comparisonToken.getComparisonType(), ComparisonType.GT));
    }

    @Test
    void equalsSameOrder_shouldBeEqual() {
        ComparisonToken first = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.EQUALS)
                .build();
        ComparisonToken second = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"))
                .firstValueType(ValueType.LOCAL_DATE)
                .secondValue("value")
                .secondValueType(ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.EQUALS)
                .build();
        assertEquals(first, second);
    }

    @Test
    void equalsDifferentOrder_shouldBeEqual() {
        ComparisonToken first = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.EQUALS)
                .build();
        ComparisonToken second = ComparisonToken.builder()
                .firstValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .comparisonType(ComparisonType.EQUALS)
                .build();
        assertEquals(first, second);
    }

    @Test
    void gtAndLtSameOrder_shouldNotBeEqual() {
        ComparisonToken first = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.GT)
                .build();
        ComparisonToken second = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.LT)
                .build();
        assertNotEquals(first, second);
    }

    @Test
    void gtAndLtDifferentOrder_shouldBeEqual() {
        ComparisonToken first = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.GT)
                .build();
        ComparisonToken second = ComparisonToken.builder()
                .firstValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .comparisonType(ComparisonType.LT)
                .build();
        assertEquals(first, second);
    }

    @Test
    void gteAndLteSameOrder_shouldNotBeEqual() {
        ComparisonToken first = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.GTE)
                .build();
        ComparisonToken second = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.LTE)
                .build();
        assertNotEquals(first, second);
    }

    @Test
    void gteAndLteDifferentOrder_shouldBeEqual() {
        ComparisonToken first = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.GTE)
                .build();
        ComparisonToken second = ComparisonToken.builder()
                .firstValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .comparisonType(ComparisonType.LTE)
                .build();
        assertEquals(first, second);
    }

    @Test
    void gtSameOrder_shouldBeEqual() {
        ComparisonToken first = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.GT)
                .build();
        ComparisonToken second = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.GT)
                .build();
        assertEquals(first, second);
    }

    @Test
    void gtDifferentOrder_shouldNotBeEqual() {
        ComparisonToken first = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.GT)
                .build();
        ComparisonToken second = ComparisonToken.builder()
                .firstValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .comparisonType(ComparisonType.GT)
                .build();
        assertNotEquals(first, second);
    }

    @Test
    void ltSameOrder_shouldBeEqual() {
        ComparisonToken first = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.LT)
                .build();
        ComparisonToken second = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.LT)
                .build();
        assertEquals(first, second);
    }

    @Test
    void ltDifferentOrder_shouldNotBeEqual() {
        ComparisonToken first = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.LT)
                .build();
        ComparisonToken second = ComparisonToken.builder()
                .firstValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .comparisonType(ComparisonType.LT)
                .build();
        assertNotEquals(first, second);
    }

    @Test
    void gteSameOrder_shouldBeEqual() {
        ComparisonToken first = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.GTE)
                .build();
        ComparisonToken second = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.GTE)
                .build();
        assertEquals(first, second);
    }

    @Test
    void gteDifferentOrder_shouldNotBeEqual() {
        ComparisonToken first = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.GTE)
                .build();
        ComparisonToken second = ComparisonToken.builder()
                .firstValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .comparisonType(ComparisonType.GTE)
                .build();
        assertNotEquals(first, second);
    }

    @Test
    void lteSameOrder_shouldBeEqual() {
        ComparisonToken first = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.LTE)
                .build();
        ComparisonToken second = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.LTE)
                .build();
        assertEquals(first, second);
    }

    @Test
    void lteDifferentOrder_shouldNotBeEqual() {
        ComparisonToken first = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.LTE)
                .build();
        ComparisonToken second = ComparisonToken.builder()
                .firstValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .secondValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .comparisonType(ComparisonType.LTE)
                .build();
        assertNotEquals(first, second);
    }

    @Test
    void equals_differentValues_shouldNotBeEqual() {
        ComparisonToken first = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2018-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.EQUALS)
                .build();
        ComparisonToken second = ComparisonToken.builder()
                .firstValue(LocalDate.parse("2100-10-10"), ValueType.LOCAL_DATE)
                .secondValue("value", ValueType.GRAPHQL_CONTEXT_FIELD_NAME)
                .comparisonType(ComparisonType.EQUALS)
                .build();
        assertNotEquals(first, second);
    }

    @Test
    void build_withoutValues_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> ComparisonToken.builder().build());
    }

    @Test
    void build_notOperationEquals_StringValueType_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> ComparisonToken.builder()
                .firstValue("value", ValueType.STRING)
                .secondValue("value", ValueType.GRAPHQL_ARGUMENT_NAME)
                .comparisonType(ComparisonType.LT)
                .build());
    }

    @Test
    void build_notOperationEquals_NullValueType_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> ComparisonToken.builder()
                .firstValue(NullValue.INSTANCE, ValueType.NULL)
                .secondValue("value", ValueType.GRAPHQL_ARGUMENT_NAME)
                .comparisonType(ComparisonType.LT)
                .build());
    }

    @Test
    void build_notOperationEquals_BooleanValueType_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> ComparisonToken.builder()
                .firstValue(true, ValueType.BOOLEAN)
                .secondValue("value", ValueType.GRAPHQL_ARGUMENT_NAME)
                .comparisonType(ComparisonType.LT)
                .build());
    }

    @Test
    void build_differentValueTypesAndNotArgumentAndNotContextVariable_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> ComparisonToken.builder()
                .firstValue(true, ValueType.BOOLEAN)
                .secondValue("value", ValueType.INTEGER)
                .comparisonType(ComparisonType.EQUALS)
                .build());
    }

    @Test
    void build_addNullFirstValue_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> ComparisonToken.builder().firstValue(null));
    }

    @Test
    void build_addNullFirstValueType_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> ComparisonToken.builder().firstValueType(null));
    }

    @Test
    void build_addNullSecondValue_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> ComparisonToken.builder().secondValue(null));
    }

    @Test
    void build_addNullSecondValueType_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> ComparisonToken.builder().secondValueType(null));
    }
}
