package ru.liboskat.graphql.security.utils;

import java.time.*;
import java.time.temporal.Temporal;

/**
 * Класс для конвертации LocalTime, LocalDate, LocalDateTime в ZonedDateTime
 */
public class TemporalToZonedDateTimeConverter {
    public static ZonedDateTime convert(Temporal value) {
        if (value instanceof ZonedDateTime) {
            return (ZonedDateTime) value;
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).atZone(ZoneId.systemDefault());
        }
        if (value instanceof LocalDate) {
            return ((LocalDate) value).atStartOfDay(ZoneId.systemDefault());
        }
        if (value instanceof LocalTime) {
            return ((LocalTime) value).atDate(LocalDate.now()).atZone(ZoneId.systemDefault());
        }
        throw new IllegalArgumentException(String.format("Can't convert %s to ZonedDateTime", value.getClass()));
    }
}
