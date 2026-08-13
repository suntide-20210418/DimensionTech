package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/** An immutable, reduced, non-negative rational number used for exact probability mass. */
public final class ExactProbability implements Comparable<ExactProbability> {
    public static final ExactProbability ZERO = new ExactProbability(BigInteger.ZERO, BigInteger.ONE);
    public static final ExactProbability ONE = new ExactProbability(BigInteger.ONE, BigInteger.ONE);

    private final BigInteger numerator;
    private final BigInteger denominator;

    private ExactProbability(BigInteger numerator, BigInteger denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public static ExactProbability of(long numerator, long denominator) {
        return of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    public static ExactProbability of(BigInteger numerator, BigInteger denominator) {
        Objects.requireNonNull(numerator, "numerator");
        Objects.requireNonNull(denominator, "denominator");
        if (numerator.signum() < 0) {
            throw new IllegalArgumentException("negative probability mass");
        }
        if (denominator.signum() <= 0) {
            throw new IllegalArgumentException("non-positive denominator");
        }
        if (numerator.signum() == 0) {
            return ZERO;
        }
        BigInteger divisor = numerator.gcd(denominator);
        BigInteger reducedNumerator = numerator.divide(divisor);
        BigInteger reducedDenominator = denominator.divide(divisor);
        if (reducedNumerator.equals(BigInteger.ONE) && reducedDenominator.equals(BigInteger.ONE)) {
            return ONE;
        }
        return new ExactProbability(reducedNumerator, reducedDenominator);
    }

    /** Exact conversion of the decimal value represented by {@link BigDecimal#valueOf(double)}. */
    public static ExactProbability fromDecimalDouble(double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException("invalid probability mass: " + value);
        }
        BigDecimal decimal = BigDecimal.valueOf(value);
        BigInteger numerator = decimal.unscaledValue();
        int scale = decimal.scale();
        return scale >= 0
                ? of(numerator, BigInteger.TEN.pow(scale))
                : of(numerator.multiply(BigInteger.TEN.pow(-scale)), BigInteger.ONE);
    }

    /** Exact rational value of a finite, non-negative IEEE-754 float. */
    public static ExactProbability fromFloat(float value) {
        if (!Float.isFinite(value) || value < 0.0F) {
            throw new IllegalArgumentException("invalid probability mass: " + value);
        }
        int bits = Float.floatToRawIntBits(value);
        int exponentBits = (bits >>> 23) & 0xff;
        int fractionBits = bits & 0x7fffff;
        if (exponentBits == 0 && fractionBits == 0) return ZERO;
        BigInteger significand = BigInteger.valueOf(
                exponentBits == 0 ? fractionBits : fractionBits | 0x800000L);
        int exponent = (exponentBits == 0 ? -126 : exponentBits - 127) - 23;
        return exponent >= 0
                ? of(significand.shiftLeft(exponent), BigInteger.ONE)
                : of(significand, BigInteger.ONE.shiftLeft(-exponent));
    }

    /** Exact rational value of a finite, non-negative IEEE-754 double. */
    public static ExactProbability fromDouble(double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException("invalid probability mass: " + value);
        }
        long bits = Double.doubleToRawLongBits(value);
        int exponentBits = (int) ((bits >>> 52) & 0x7ffL);
        long fractionBits = bits & 0xfffffffffffffL;
        if (exponentBits == 0 && fractionBits == 0L) return ZERO;
        BigInteger significand = BigInteger.valueOf(
                exponentBits == 0 ? fractionBits : fractionBits | 0x10000000000000L);
        int exponent = (exponentBits == 0 ? -1022 : exponentBits - 1023) - 52;
        return exponent >= 0
                ? of(significand.shiftLeft(exponent), BigInteger.ONE)
                : of(significand, BigInteger.ONE.shiftLeft(-exponent));
    }

    public ExactProbability add(ExactProbability other) {
        Objects.requireNonNull(other, "other");
        return of(
                numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
                denominator.multiply(other.denominator));
    }

    public ExactProbability multiply(ExactProbability other) {
        Objects.requireNonNull(other, "other");
        return of(numerator.multiply(other.numerator), denominator.multiply(other.denominator));
    }

    public ExactProbability subtract(ExactProbability other) {
        Objects.requireNonNull(other, "other");
        BigInteger difference =
                numerator.multiply(other.denominator)
                        .subtract(other.numerator.multiply(denominator));
        if (difference.signum() < 0) {
            throw new ArithmeticException("negative probability result");
        }
        return of(difference, denominator.multiply(other.denominator));
    }

    public ExactProbability divide(ExactProbability other) {
        Objects.requireNonNull(other, "other");
        if (other.isZero()) {
            throw new ArithmeticException("division by zero");
        }
        return of(numerator.multiply(other.denominator), denominator.multiply(other.numerator));
    }

    public boolean isZero() {
        return numerator.signum() == 0;
    }

    public BigInteger numerator() {
        return numerator;
    }

    public BigInteger denominator() {
        return denominator;
    }

    public double doubleValue() {
        return numerator.doubleValue() / denominator.doubleValue();
    }

    @Override
    public int compareTo(ExactProbability other) {
        return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator));
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof ExactProbability other
                && numerator.equals(other.numerator)
                && denominator.equals(other.denominator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numerator, denominator);
    }

    @Override
    public String toString() {
        return denominator.equals(BigInteger.ONE) ? numerator.toString() : numerator + "/" + denominator;
    }
}
