package ru.liboskat.graphql.security.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.liboskat.graphql.security.utils.NumberComparator;

import java.math.BigDecimal;
import java.math.BigInteger;

class NumberComparatorTests {
    private final NumberComparator numberComparator = new NumberComparator();

    @Test
    void compareSameIntegerAndInteger_shouldReturnZero() {
        Integer first = 1;
        Integer second = 1;
        Assertions.assertEquals(numberComparator.compare(first, second), 0);
    }

    @Test
    void compareSameIntegerAndDouble_shouldReturnZero() {
        Integer first = 1;
        Double second = 1.0;
        Assertions.assertEquals(numberComparator.compare(first, second), 0);
    }

    @Test
    void compareSameIntegerAndBigInteger_shouldReturnZero() {
        Integer first = 1;
        BigInteger second = new BigInteger("1");
        Assertions.assertEquals(numberComparator.compare(first, second), 0);
    }

    @Test
    void compareSameIntegerAndBigDecimal_shouldReturnZero() {
        Integer first = 1;
        BigDecimal second = new BigDecimal("1.0");
        Assertions.assertEquals(numberComparator.compare(first, second), 0);
    }

    @Test
    void compareSameDoubleAndDouble_shouldReturnZero() {
        Double first = 1.0;
        Double second = 1.0;
        Assertions.assertEquals(numberComparator.compare(first, second), 0);
    }

    @Test
    void compareSameDoubleAndBigInteger_shouldReturnZero() {
        Double first = 1.0;
        BigInteger second = new BigInteger("1");
        Assertions.assertEquals(numberComparator.compare(first, second), 0);
    }

    @Test
    void compareSameDoubleAndBigDecimal_shouldReturnZero() {
        Double first = 1.0;
        BigDecimal second = new BigDecimal("1.0");
        Assertions.assertEquals(numberComparator.compare(first, second), 0);
    }

    @Test
    void compareSameBigIntegerAndBigInteger_shouldReturnZero() {
        BigInteger first = new BigInteger("1");
        BigInteger second = new BigInteger("1");
        Assertions.assertEquals(numberComparator.compare(first, second), 0);
    }

    @Test
    void compareSameBigIntegerAndBigDecimal_shouldReturnZero() {
        BigInteger first = new BigInteger("1");
        BigDecimal second = new BigDecimal("1.0");
        Assertions.assertEquals(numberComparator.compare(first, second), 0);
    }

    @Test
    void compareSameBigDecimalAndBigDecimal_shouldReturnZero() {
        BigDecimal first = new BigDecimal("1.0");
        BigDecimal second = new BigDecimal("1.0");
        Assertions.assertEquals(numberComparator.compare(first, second), 0);
    }

    @Test
    void compareIntegerBiggerThanInteger_shouldReturnPositive() {
        Integer first = 2;
        Integer second = 1;
        Assertions.assertTrue(numberComparator.compare(first, second) > 0);
    }

    @Test
    void compareIntegerBiggerThanDouble_shouldReturnPositive() {
        Integer first = 2;
        Double second = 1.0;
        Assertions.assertTrue(numberComparator.compare(first, second) > 0);
    }

    @Test
    void compareIntegerBiggerThanBigInteger_shouldReturnPositive() {
        Integer first = 2;
        BigInteger second = new BigInteger("1");
        Assertions.assertTrue(numberComparator.compare(first, second) > 0);
    }

    @Test
    void compareIntegerBiggerThanBigDecimal_shouldReturnPositive() {
        Integer first = 2;
        BigDecimal second = new BigDecimal("1.0");
        Assertions.assertTrue(numberComparator.compare(first, second) > 0);
    }

    @Test
    void compareDoubleBiggerThanDouble_shouldReturnPositive() {
        Double first = 2.0;
        Double second = 1.0;
        Assertions.assertTrue(numberComparator.compare(first, second) > 0);
    }

    @Test
    void compareDoubleBiggerThanBigInteger_shouldReturnPositive() {
        Double first = 2.0;
        BigInteger second = new BigInteger("1");
        Assertions.assertTrue(numberComparator.compare(first, second) > 0);
    }

    @Test
    void compareDoubleBiggerThanBigDecimal_shouldReturnPositive() {
        Double first = 2.0;
        BigDecimal second = new BigDecimal("1.0");
        Assertions.assertTrue(numberComparator.compare(first, second) > 0);
    }

    @Test
    void compareBigIntegerBiggerThanBigInteger_shouldReturnPositive() {
        BigInteger first = new BigInteger("2");
        BigInteger second = new BigInteger("1");
        Assertions.assertTrue(numberComparator.compare(first, second) > 0);
    }

    @Test
    void compareBigIntegerBiggerThanBigDecimal_shouldReturnPositive() {
        BigInteger first = new BigInteger("2");
        BigDecimal second = new BigDecimal("1.0");
        Assertions.assertTrue(numberComparator.compare(first, second) > 0);
    }

    @Test
    void compareBigDecimalBiggerThanBigDecimal_shouldReturnPositive() {
        BigDecimal first = new BigDecimal("2.0");
        BigDecimal second = new BigDecimal("1.0");
        Assertions.assertTrue(numberComparator.compare(first, second) > 0);
    }

    @Test
    void compareIntegerSmallerThanInteger_shouldReturnNegative() {
        Integer first = 1;
        Integer second = 2;
        Assertions.assertTrue(numberComparator.compare(first, second) < 0);
    }

    @Test
    void compareIntegerSmallerThanDouble_shouldReturnNegative() {
        Integer first = 1;
        Double second = 2.0;
        Assertions.assertTrue(numberComparator.compare(first, second) < 0);
    }

    @Test
    void compareIntegerSmallerThanBigInteger_shouldReturnNegative() {
        Integer first = 1;
        BigInteger second = new BigInteger("2");
        Assertions.assertTrue(numberComparator.compare(first, second) < 0);
    }

    @Test
    void compareIntegerSmallerThanBigDecimal_shouldReturnNegative() {
        Integer first = 1;
        BigDecimal second = new BigDecimal("2.0");
        Assertions.assertTrue(numberComparator.compare(first, second) < 0);
    }

    @Test
    void compareDoubleSmallerThanDouble_shouldReturnNegative() {
        Double first = 1.0;
        Double second = 2.0;
        Assertions.assertTrue(numberComparator.compare(first, second) < 0);
    }

    @Test
    void compareDoubleSmallerThanBigInteger_shouldReturnNegative() {
        Double first = 1.0;
        BigInteger second = new BigInteger("2");
        Assertions.assertTrue(numberComparator.compare(first, second) < 0);
    }

    @Test
    void compareDoubleSmallerThanBigDecimal_shouldReturnNegative() {
        Double first = 1.0;
        BigDecimal second = new BigDecimal("2.0");
        Assertions.assertTrue(numberComparator.compare(first, second) < 0);
    }

    @Test
    void compareBigIntegerSmallerThanBigInteger_shouldReturnNegative() {
        BigInteger first = new BigInteger("1");
        BigInteger second = new BigInteger("2");
        Assertions.assertTrue(numberComparator.compare(first, second) < 0);
    }

    @Test
    void compareBigIntegerSmallerThanBigDecimal_shouldReturnNegative() {
        BigInteger first = new BigInteger("1");
        BigDecimal second = new BigDecimal("2.0");
        Assertions.assertTrue(numberComparator.compare(first, second) < 0);
    }

    @Test
    void compareBigDecimalSmallerThanBigDecimal_shouldReturnNegative() {
        BigDecimal first = new BigDecimal("1.0");
        BigDecimal second = new BigDecimal("2.0");
        Assertions.assertTrue(numberComparator.compare(first, second) < 0);
    }

    @Test
    void compareIllegalNumber_shouldThrowException() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                numberComparator.compare(new OwnNumber(), new OwnNumber()));
    }

    private static class OwnNumber extends Number {
        @Override
        public int intValue() {
            return 0;
        }

        @Override
        public long longValue() {
            return 0;
        }

        @Override
        public float floatValue() {
            return 0;
        }

        @Override
        public double doubleValue() {
            return 0;
        }
    }
}
