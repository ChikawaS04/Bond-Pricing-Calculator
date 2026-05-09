package com.fixedIncome.daycount;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Actual/Actual ICMA (ISMA) day-count convention.
 *
 * <p>Used for Eurobonds and most international fixed-rate bonds. The year fraction
 * is computed as the actual number of days in the sub-period divided by the actual
 * number of days in the reference coupon period scaled by the payment frequency:</p>
 *
 * <pre>
 *   yearFraction(start, end) = actualDays(start, end) / (frequency * actualDays(refStart, refEnd))
 * </pre>
 *
 * <p>The reference period is taken as the natural coupon period beginning at
 * {@code start} — i.e., {@code [start, start + 12/frequency months]}. This gives
 * an exact result of {@code 1/frequency} for full coupon periods, and is always
 * used as a ratio in the pricer (where both numerator and denominator share the
 * same coupon period), so fractional-period results are also correct.</p>
 *
 * <p>The {@code frequency} parameter must match the bond's payment frequency
 * (e.g., 2 for semi-annual, 4 for quarterly). Use
 * {@link DayCountFactory#getConvention(String, int)} to obtain a correctly
 * configured instance.</p>
 */
public class ActActICMA implements DayCountConvention {

    private final int frequency;

    /**
     * Constructs an Act/Act ICMA instance for a given payment frequency.
     *
     * @param frequency coupon payments per year (e.g. 2 for semi-annual, 4 for quarterly)
     * @throws IllegalArgumentException if {@code frequency} is not positive
     */
    public ActActICMA(int frequency) {
        if (frequency <= 0) {
            throw new IllegalArgumentException("Payment frequency must be > 0, got: " + frequency);
        }
        this.frequency = frequency;
    }

    /**
     * Calculates the year fraction under Act/Act ICMA.
     *
     * <p>The reference period is {@code [start, start + 12/frequency months]}.
     * Returns {@code 0.0} if {@code start} equals {@code end}.</p>
     *
     * @param start the start date (inclusive)
     * @param end   the end date (exclusive)
     * @return year fraction
     * @throws IllegalArgumentException if {@code end} is before {@code start}
     */
    @Override
    public double yearFraction(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must not be before start date");
        }
        if (start.equals(end)) {
            return 0.0;
        }

        long actualDays = ChronoUnit.DAYS.between(start, end);

        // Reference period: the natural coupon period starting at 'start'
        LocalDate refEnd = start.plusMonths(12 / frequency);
        long refDays = ChronoUnit.DAYS.between(start, refEnd);

        return (double) actualDays / (frequency * refDays);
    }

    /** @return {@code "ACT/ACT ICMA"} */
    @Override
    public String getName() {
        return "ACT/ACT ICMA";
    }
}
