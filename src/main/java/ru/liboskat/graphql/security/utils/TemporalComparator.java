package ru.liboskat.graphql.security.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Comparator;

/**
 * Implementation of {@link Comparator} that compares {@link LocalDate}, {@link LocalTime}, {@link LocalDateTime},
 * {@link ZonedDateTime}
 */
public class TemporalComparator implements Comparator<Temporal> {
    @Override
    public int compare(Temporal first, Temporal second) {
        if (first instanceof ZonedDateTime) {
            ZonedDateTime firstDateTime = (ZonedDateTime) first;
            if (second instanceof ZonedDateTime) {
                return firstDateTime.compareTo((ZonedDateTime) second);
            }
            if (second instanceof LocalDateTime) {
                return firstDateTime.toLocalDateTime().compareTo((LocalDateTime) second);
            }
            if (second instanceof LocalDate) {
                return firstDateTime.toLocalDate().compareTo((LocalDate) second);
            }
            if (second instanceof LocalTime) {
                return firstDateTime.toLocalTime().compareTo((LocalTime) second);
            }
            throw new IllegalArgumentException(String.format("Can't compare %s and %s", first, second));
        }
        if (first instanceof LocalDateTime) {
            LocalDateTime firstDateTime = (LocalDateTime) first;
            if (second instanceof ZonedDateTime) {
                return firstDateTime.compareTo(((ZonedDateTime) second).toLocalDateTime());
            }
            if (second instanceof LocalDateTime) {
                return firstDateTime.compareTo((LocalDateTime) second);
            }
            if (second instanceof LocalDate) {
                return firstDateTime.toLocalDate().compareTo((LocalDate) second);
            }
            if (second instanceof LocalTime) {
                return firstDateTime.toLocalTime().compareTo((LocalTime) second);
            }
            throw new IllegalArgumentException(String.format("Can't compare %s and %s", first, second));
        }
        if (first instanceof LocalDate) {
            LocalDate firstDate = (LocalDate) first;
            if (second instanceof ZonedDateTime) {
                return firstDate.compareTo(((ZonedDateTime) second).toLocalDate());
            }
            if (second instanceof LocalDateTime) {
                return firstDate.compareTo(((LocalDateTime) second).toLocalDate());
            }
            if (second instanceof LocalDate) {
                return firstDate.compareTo((LocalDate) second);
            }
            throw new IllegalArgumentException(String.format("Can't compare %s and %s", first, second));
        }
        if (first instanceof LocalTime) {
            LocalTime firstTime = (LocalTime) first;
            if (second instanceof ZonedDateTime) {
                return firstTime.compareTo(((ZonedDateTime) second).toLocalTime());
            }
            if (second instanceof LocalDateTime) {
                return firstTime.compareTo(((LocalDateTime) second).toLocalTime());
            }
            if (second instanceof LocalTime) {
                return firstTime.compareTo((LocalTime) second);
            }
            throw new IllegalArgumentException(String.format("Can't compare %s and %s", first, second));
        }
        throw new IllegalArgumentException(String.format("Can't compare %s and %s", first, second));
    }
}
