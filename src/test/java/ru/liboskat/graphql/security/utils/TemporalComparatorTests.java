package ru.liboskat.graphql.security.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.liboskat.graphql.security.utils.TemporalComparator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

class TemporalComparatorTests {
    private final TemporalComparator temporalComparator = new TemporalComparator();

    @Test
    void compareSameZonedDateTimeAndZonedDateTime_shouldReturnZero() {
        ZonedDateTime first = ZonedDateTime.parse("2001-01-01T00:00+00:00");
        ZonedDateTime second = ZonedDateTime.parse("2001-01-01T00:00+00:00");
        Assertions.assertEquals(temporalComparator.compare(first, second), 0);
    }

    @Test
    void compareSameZonedDateTimeAndLocalDateTime_shouldReturnZero() {
        ZonedDateTime first = ZonedDateTime.parse("2001-01-01T00:00+00:00");
        LocalDateTime second = LocalDateTime.parse("2001-01-01T00:00");
        Assertions.assertEquals(temporalComparator.compare(first, second), 0);
    }

    @Test
    void compareSameZonedDateTimeAndLocalDate_shouldReturnZero() {
        ZonedDateTime first = ZonedDateTime.parse("2001-01-01T00:00+00:00");
        LocalDate second = LocalDate.parse("2001-01-01");
        Assertions.assertEquals(temporalComparator.compare(first, second), 0);
    }

    @Test
    void compareSameZonedDateTimeAndLocalTime_shouldReturnZero() {
        ZonedDateTime first = ZonedDateTime.parse("2001-01-01T00:00+00:00");
        LocalTime second = LocalTime.parse("00:00");
        Assertions.assertEquals(temporalComparator.compare(first, second), 0);
    }

    @Test
    void compareSameLocalDateTimeAndLocalDateTime_shouldReturnZero() {
        LocalDateTime first = LocalDateTime.parse("2001-01-01T00:00");
        LocalDateTime second = LocalDateTime.parse("2001-01-01T00:00");
        Assertions.assertEquals(temporalComparator.compare(first, second), 0);
    }

    @Test
    void compareSameLocalDateTimeAndLocalDate_shouldReturnZero() {
        LocalDateTime first = LocalDateTime.parse("2001-01-01T00:00");
        LocalDate second = LocalDate.parse("2001-01-01");
        Assertions.assertEquals(temporalComparator.compare(first, second), 0);
    }

    @Test
    void compareSameLocalDateTimeAndLocalTime_shouldReturnZero() {
        LocalDateTime first = LocalDateTime.parse("2001-01-01T00:00");
        LocalTime second = LocalTime.parse("00:00");
        Assertions.assertEquals(temporalComparator.compare(first, second), 0);
    }

    @Test
    void compareSameLocalDateAndLocalDate_shouldReturnZero() {
        LocalDate first = LocalDate.parse("2001-01-01");
        LocalDate second = LocalDate.parse("2001-01-01");
        Assertions.assertEquals(temporalComparator.compare(first, second), 0);
    }

    @Test
    void compareLocalDateAndLocalTime_shouldThrowException() {
        LocalDate first = LocalDate.parse("2001-01-01");
        LocalTime second = LocalTime.parse("00:00");
        Assertions.assertThrows(IllegalArgumentException.class, () -> temporalComparator.compare(first, second));
    }

    @Test
    void compareSameLocalTimeAndLocalTime_shouldReturnZero() {
        LocalTime first = LocalTime.parse("00:00");
        LocalTime second = LocalTime.parse("00:00");
        Assertions.assertEquals(temporalComparator.compare(first, second), 0);
    }

    @Test
    void compareZonedDateTimeBiggerThanZonedDateTime_shouldReturnPositive() {
        ZonedDateTime first = ZonedDateTime.parse("2101-01-01T00:00+00:00");
        ZonedDateTime second = ZonedDateTime.parse("2001-01-01T00:00+00:00");
        Assertions.assertTrue(temporalComparator.compare(first, second) > 0);
    }

    @Test
    void compareZonedDateTimeBiggerThanLocalDateTime_shouldReturnPositive() {
        ZonedDateTime first = ZonedDateTime.parse("2101-01-01T00:00+00:00");
        LocalDateTime second = LocalDateTime.parse("2001-01-01T00:00");
        Assertions.assertTrue(temporalComparator.compare(first, second) > 0);
    }

    @Test
    void compareZonedDateTimeBiggerThanLocalDate_shouldReturnPositive() {
        ZonedDateTime first = ZonedDateTime.parse("2101-01-01T00:00+00:00");
        LocalDate second = LocalDate.parse("2001-01-01");
        Assertions.assertTrue(temporalComparator.compare(first, second) > 0);
    }

    @Test
    void compareZonedDateTimeBiggerThanLocalTime_shouldReturnPositive() {
        ZonedDateTime first = ZonedDateTime.parse("2101-01-01T12:00+00:00");
        LocalTime second = LocalTime.parse("00:00");
        Assertions.assertTrue(temporalComparator.compare(first, second) > 0);
    }

    @Test
    void compareLocalDateTimeBiggerThanLocalDateTime_shouldReturnPositive() {
        LocalDateTime first = LocalDateTime.parse("2101-01-01T00:00");
        LocalDateTime second = LocalDateTime.parse("2001-01-01T00:00");
        Assertions.assertTrue(temporalComparator.compare(first, second) > 0);
    }

    @Test
    void compareLocalDateTimeBiggerThanLocalDate_shouldReturnPositive() {
        LocalDateTime first = LocalDateTime.parse("2101-01-01T00:00");
        LocalDate second = LocalDate.parse("2001-01-01");
        Assertions.assertTrue(temporalComparator.compare(first, second) > 0);
    }

    @Test
    void compareLocalDateTimeBiggerThanLocalTime_shouldReturnPositive() {
        LocalDateTime first = LocalDateTime.parse("2101-01-01T12:00");
        LocalTime second = LocalTime.parse("00:00");
        Assertions.assertTrue(temporalComparator.compare(first, second) > 0);
    }

    @Test
    void compareLocalDateBiggerThanLocalDate_shouldReturnPositive() {
        LocalDate first = LocalDate.parse("2101-01-01");
        LocalDate second = LocalDate.parse("2001-01-01");
        Assertions.assertTrue(temporalComparator.compare(first, second) > 0);
    }

    @Test
    void compareLocalTimeBiggerThanLocalTime_shouldReturnPositive() {
        LocalTime first = LocalTime.parse("12:00");
        LocalTime second = LocalTime.parse("00:00");
        Assertions.assertTrue(temporalComparator.compare(first, second) > 0);
    }

    @Test
    void compareZonedDateTimeSmallerThanZonedDateTime_shouldReturnNegative() {
        ZonedDateTime first = ZonedDateTime.parse("2001-01-01T00:00+00:00");
        ZonedDateTime second = ZonedDateTime.parse("2101-01-01T00:00+00:00");
        Assertions.assertTrue(temporalComparator.compare(first, second) < 0);
    }

    @Test
    void compareZonedDateTimeSmallerThanLocalDateTime_shouldReturnNegative() {
        ZonedDateTime first = ZonedDateTime.parse("2001-01-01T00:00+00:00");
        LocalDateTime second = LocalDateTime.parse("2101-01-01T00:00");
        Assertions.assertTrue(temporalComparator.compare(first, second) < 0);
    }

    @Test
    void compareZonedDateTimeSmallerThanLocalDate_shouldReturnNegative() {
        ZonedDateTime first = ZonedDateTime.parse("2001-01-01T00:00+00:00");
        LocalDate second = LocalDate.parse("2101-01-01");
        Assertions.assertTrue(temporalComparator.compare(first, second) < 0);
    }

    @Test
    void compareZonedDateTimeSmallerThanLocalTime_shouldReturnNegative() {
        ZonedDateTime first = ZonedDateTime.parse("2001-01-01T00:00+00:00");
        LocalTime second = LocalTime.parse("12:00");
        Assertions.assertTrue(temporalComparator.compare(first, second) < 0);
    }

    @Test
    void compareLocalDateTimeSmallerThanLocalDateTime_shouldReturnNegative() {
        LocalDateTime first = LocalDateTime.parse("2001-01-01T00:00");
        LocalDateTime second = LocalDateTime.parse("2101-01-01T00:00");
        Assertions.assertTrue(temporalComparator.compare(first, second) < 0);
    }

    @Test
    void compareLocalDateTimeSmallerThanLocalDate_shouldReturnNegative() {
        LocalDateTime first = LocalDateTime.parse("2001-01-01T00:00");
        LocalDate second = LocalDate.parse("2101-01-01");
        Assertions.assertTrue(temporalComparator.compare(first, second) < 0);
    }

    @Test
    void compareLocalDateTimeSmallerThanLocalTime_shouldReturnNegative() {
        LocalDateTime first = LocalDateTime.parse("2001-01-01T00:00");
        LocalTime second = LocalTime.parse("12:00");
        Assertions.assertTrue(temporalComparator.compare(first, second) < 0);
    }

    @Test
    void compareLocalDateSmallerThanLocalDate_shouldReturnNegative() {
        LocalDate first = LocalDate.parse("2001-01-01");
        LocalDate second = LocalDate.parse("2101-01-01");
        Assertions.assertTrue(temporalComparator.compare(first, second) < 0);
    }

    @Test
    void compareLocalTimeSmallerThanLocalTime_shouldReturnNegative() {
        LocalTime first = LocalTime.parse("00:00");
        LocalTime second = LocalTime.parse("12:00");
        Assertions.assertTrue(temporalComparator.compare(first, second) < 0);
    }
}
