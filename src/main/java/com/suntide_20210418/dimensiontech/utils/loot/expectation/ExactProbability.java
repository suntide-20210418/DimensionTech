package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/** An immutable, reduced, non-negative rational number used for exact probability mass. */
public final class ExactProbability implements Comparable<ExactProbability> {
    public static final ExactProbability ZERO =
            new ExactProbability(BigInteger.ZERO, BigInteger.ONE);
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
        BigInteger significand =
                BigInteger.valueOf(exponentBits == 0 ? fractionBits : fractionBits | 0x800000L);
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
        BigInteger significand =
                BigInteger.valueOf(
                        exponentBits == 0 ? fractionBits : fractionBits | 0x10000000000000L);
        int exponent = (exponentBits == 0 ? -1022 : exponentBits - 1023) - 52;
        return exponent >= 0
                ? of(significand.shiftLeft(exponent), BigInteger.ONE)
                : of(significand, BigInteger.ONE.shiftLeft(-exponent));
    }

    public ExactProbability add(ExactProbability other) {
        Objects.requireNonNull(other, "other");
        if (isZero()) return other;
        if (other.isZero()) return this;
        BigInteger common = denominator.gcd(other.denominator);
        BigInteger leftScale = other.denominator.divide(common);
        BigInteger rightScale = denominator.divide(common);
        BigInteger sum = numerator.multiply(leftScale).add(other.numerator.multiply(rightScale));
        BigInteger reduction = sum.gcd(common);
        return new ExactProbability(
                sum.divide(reduction), rightScale.multiply(other.denominator.divide(reduction)));
    }

    public ExactProbability multiply(ExactProbability other) {
        Objects.requireNonNull(other, "other");
        if (isZero() || other.isZero()) return ZERO;
        if (equals(ONE)) return other;
        if (other.equals(ONE)) return this;
        BigInteger leftReduction = numerator.gcd(other.denominator);
        BigInteger rightReduction = other.numerator.gcd(denominator);
        return new ExactProbability(
                numerator.divide(leftReduction).multiply(other.numerator.divide(rightReduction)),
                denominator
                        .divide(rightReduction)
                        .multiply(other.denominator.divide(leftReduction)));
    }

    public ExactProbability subtract(ExactProbability other) {
        Objects.requireNonNull(other, "other");
        if (other.isZero()) return this;
        BigInteger common = denominator.gcd(other.denominator);
        BigInteger leftScale = other.denominator.divide(common);
        BigInteger rightScale = denominator.divide(common);
        BigInteger difference =
                numerator.multiply(leftScale).subtract(other.numerator.multiply(rightScale));
        if (difference.signum() < 0) {
            throw new ArithmeticException("negative probability result");
        }
        if (difference.signum() == 0) return ZERO;
        BigInteger reduction = difference.gcd(common);
        return new ExactProbability(
                difference.divide(reduction),
                rightScale.multiply(other.denominator.divide(reduction)));
    }

    public ExactProbability divide(ExactProbability other) {
        Objects.requireNonNull(other, "other");
        if (other.isZero()) {
            throw new ArithmeticException("division by zero");
        }
        if (isZero()) return ZERO;
        BigInteger numeratorReduction = numerator.gcd(other.numerator);
        BigInteger denominatorReduction = other.denominator.gcd(denominator);
        return new ExactProbability(
                numerator
                        .divide(numeratorReduction)
                        .multiply(other.denominator.divide(denominatorReduction)),
                denominator
                        .divide(denominatorReduction)
                        .multiply(other.numerator.divide(numeratorReduction)));
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
        if (isZero()) {
            return 0.0D;
        }

        /*
         * Do not convert the two BigIntegers independently.  Both operands can be larger than
         * Double.MAX_VALUE even when their quotient is an ordinary finite value; the naive
         * `numerator.doubleValue() / denominator.doubleValue()` then produces Infinity / Infinity
         * (NaN).  Rounding the rational directly also keeps this terminal conversion independent
         * of the size of the exact intermediate state.
         */
        int exponent = binaryExponent();
        if (exponent > 1023) {
            return Double.POSITIVE_INFINITY;
        }
        if (exponent < -1022) {
            BigInteger significand = roundedScaled(1074);
            if (significand.signum() == 0) {
                return 0.0D;
            }
            BigInteger normalThreshold = BigInteger.ONE.shiftLeft(52);
            if (significand.compareTo(normalThreshold) >= 0) {
                return Double.MIN_NORMAL;
            }
            return Double.longBitsToDouble(significand.longValue());
        }

        BigInteger significand = roundedScaled(52 - exponent);
        BigInteger carryThreshold = BigInteger.ONE.shiftLeft(53);
        if (significand.compareTo(carryThreshold) >= 0) {
            significand = BigInteger.ONE.shiftLeft(52);
            exponent++;
            if (exponent > 1023) {
                return Double.POSITIVE_INFINITY;
            }
        }
        long exponentBits = (long) (exponent + 1023);
        long fractionBits = significand.subtract(BigInteger.ONE.shiftLeft(52)).longValue();
        return Double.longBitsToDouble((exponentBits << 52) | fractionBits);
    }

    /** Converts this exact value at a terminal double boundary, rejecting overflow. */
    public double finiteDoubleValue() {
        double value = doubleValue();
        if (!Double.isFinite(value)) {
            throw new ArithmeticException("exact value does not fit in a finite double");
        }
        return value;
    }

    private int binaryExponent() {
        int candidate = numerator.bitLength() - denominator.bitLength();
        int comparison;
        if (candidate >= 0) {
            comparison = numerator.compareTo(denominator.shiftLeft(candidate));
        } else {
            comparison = numerator.shiftLeft(-candidate).compareTo(denominator);
        }
        return comparison >= 0 ? candidate : candidate - 1;
    }

    /** Returns round-to-nearest-even(numerator / denominator * 2^shift). */
    private BigInteger roundedScaled(int shift) {
        BigInteger scaledNumerator;
        BigInteger scaledDenominator;
        if (shift >= 0) {
            scaledNumerator = numerator.shiftLeft(shift);
            scaledDenominator = denominator;
        } else {
            scaledNumerator = numerator;
            scaledDenominator = denominator.shiftLeft(-shift);
        }
        BigInteger[] quotientAndRemainder = scaledNumerator.divideAndRemainder(scaledDenominator);
        BigInteger quotient = quotientAndRemainder[0];
        BigInteger remainder = quotientAndRemainder[1];
        if (remainder.signum() == 0) {
            return quotient;
        }
        int comparison = remainder.shiftLeft(1).compareTo(scaledDenominator);
        return comparison > 0 || (comparison == 0 && quotient.testBit(0))
                ? quotient.add(BigInteger.ONE)
                : quotient;
    }

    @Override
    public int compareTo(ExactProbability other) {
        return numerator
                .multiply(other.denominator)
                .compareTo(other.numerator.multiply(denominator));
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
        return denominator.equals(BigInteger.ONE)
                ? numerator.toString()
                : numerator + "/" + denominator;
    }
}
