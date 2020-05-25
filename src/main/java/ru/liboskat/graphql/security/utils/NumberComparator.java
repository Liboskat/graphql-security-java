package ru.liboskat.graphql.security.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class NumberComparator implements Comparator<Number> {
    @Override
    public int compare(Number o1, Number o2) {
        if (o1 instanceof Byte || o1 instanceof Short || o1 instanceof Integer || o1 instanceof Long ||
                o1 instanceof AtomicInteger || o1 instanceof AtomicLong) {
            if (o2 instanceof Byte || o2 instanceof Short || o2 instanceof Integer || o2 instanceof Long ||
                    o2 instanceof AtomicInteger || o2 instanceof AtomicLong) {
                return Long.compare(o1.longValue(), o2.longValue());
            }
            if (o2 instanceof Float || o2 instanceof Double) {
                return Double.compare(o1.doubleValue(), o2.doubleValue());
            }
            if (o2 instanceof BigInteger) {
                return BigInteger.valueOf(o1.longValue()).compareTo((BigInteger) o2);
            }
            if (o2 instanceof BigDecimal) {
                return BigDecimal.valueOf(o1.doubleValue()).compareTo((BigDecimal) o2);
            }
            throw new IllegalArgumentException(String.format("Can't compare %s and %s", o1, o2));
        }
        if (o1 instanceof Float || o1 instanceof Double) {
            if (o2 instanceof Byte || o2 instanceof Short || o2 instanceof Integer || o2 instanceof Long ||
                    o2 instanceof AtomicInteger || o2 instanceof AtomicLong) {
                return Double.compare(o1.doubleValue(), o2.doubleValue());
            }
            if (o2 instanceof Float || o2 instanceof Double) {
                return Double.compare(o1.doubleValue(), o2.doubleValue());
            }
            if (o2 instanceof BigInteger) {
                return BigDecimal.valueOf(o1.doubleValue()).compareTo(new BigDecimal((BigInteger) o2));
            }
            if (o2 instanceof BigDecimal) {
                return BigDecimal.valueOf(o1.doubleValue()).compareTo((BigDecimal) o2);
            }
            throw new IllegalArgumentException(String.format("Can't compare %s and %s", o1, o2));
        }
        if (o1 instanceof BigInteger) {
            BigInteger o1BigInteger = (BigInteger) o1;
            if (o2 instanceof Byte || o2 instanceof Short || o2 instanceof Integer || o2 instanceof Long ||
                    o2 instanceof AtomicInteger || o2 instanceof AtomicLong) {
                return o1BigInteger.compareTo(BigInteger.valueOf(o1.longValue()));
            }
            if (o2 instanceof Float || o2 instanceof Double) {
                return new BigDecimal(o1BigInteger).compareTo(BigDecimal.valueOf(o2.doubleValue()));
            }
            if (o2 instanceof BigInteger) {
                return o1BigInteger.compareTo((BigInteger) o2);
            }
            if (o2 instanceof BigDecimal) {
                return new BigDecimal(o1BigInteger).compareTo((BigDecimal) o2);
            }
            throw new IllegalArgumentException(String.format("Can't compare %s and %s", o1, o2));
        }
        if (o1 instanceof BigDecimal) {
            BigDecimal o1BigDecimal = (BigDecimal) o1;
            if (o2 instanceof Byte || o2 instanceof Short || o2 instanceof Integer || o2 instanceof Long ||
                    o2 instanceof AtomicInteger || o2 instanceof AtomicLong) {
                return o1BigDecimal.compareTo(BigDecimal.valueOf(o1.doubleValue()));
            }
            if (o2 instanceof Float || o2 instanceof Double) {
                return o1BigDecimal.compareTo(BigDecimal.valueOf(o2.doubleValue()));
            }
            if (o2 instanceof BigInteger) {
                return o1BigDecimal.compareTo(new BigDecimal((BigInteger) o2));
            }
            if (o2 instanceof BigDecimal) {
                return o1BigDecimal.compareTo((BigDecimal) o2);
            }
            throw new IllegalArgumentException(String.format("Can't compare %s and %s", o1, o2));
        }
        throw new IllegalArgumentException(String.format("Can't compare %s and %s", o1, o2));
    }
}
