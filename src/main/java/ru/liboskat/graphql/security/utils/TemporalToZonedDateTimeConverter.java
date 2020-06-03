package ru.liboskat.graphql.security.utils;

import java.time.*;
import java.time.temporal.Temporal;

/**
 * Класс для конвертации {@link LocalTime}, {@link LocalDate}, {@link LocalDateTime} в {@link ZonedDateTime}
 */
public class TemporalToZonedDateTimeConverter {
    /**
     * Конвертирует {@link LocalTime}, {@link LocalDate}, {@link LocalDateTime} в {@link ZonedDateTime}
     *
     * @param value объект даты/времени {@link java.time}
     * @return объект, сконвертированный в {@link ZonedDateTime}
     * @throws IllegalArgumentException, если тип не является одним из {@link LocalTime}, {@link LocalDate},
     *                                   {@link LocalDateTime}, {@link ZonedDateTime}
     */
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
