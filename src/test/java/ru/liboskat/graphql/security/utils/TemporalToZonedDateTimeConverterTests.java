package ru.liboskat.graphql.security.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.*;

class TemporalToZonedDateTimeConverterTests {
    @Test
    void convert_ZonedDateTime_shouldReturnCorrect() {
        ZonedDateTime expected = ZonedDateTime
                .of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime zonedDateTime = ZonedDateTime
                .of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        Assertions.assertEquals(expected, TemporalToZonedDateTimeConverter.convert(zonedDateTime));
    }

    @Test
    void convert_LocalDateTime_shouldReturnCorrect() {
        ZonedDateTime expected = ZonedDateTime
                .of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        LocalDateTime localDateTime = LocalDateTime
                .of(2000, 1, 1, 0, 0, 0, 0);
        Assertions.assertEquals(expected, TemporalToZonedDateTimeConverter.convert(localDateTime));
    }

    @Test
    void convert_LocalDate_shouldReturnCorrect() {
        ZonedDateTime expected = ZonedDateTime
                .of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        LocalDate localDate = LocalDate.of(2000, 1, 1);
        Assertions.assertEquals(expected, TemporalToZonedDateTimeConverter.convert(localDate));
    }

    @Test
    void convert_LocalTime_shouldReturnCorrect() {
        LocalDateTime localDateTime = LocalDateTime.now();
        ZonedDateTime expected = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
        Assertions.assertEquals(expected, TemporalToZonedDateTimeConverter.convert(localDateTime.toLocalTime()));
    }

    @Test
    void convert_IllegalType_shouldThrowException() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> TemporalToZonedDateTimeConverter.convert(Instant.now()));
    }
}
