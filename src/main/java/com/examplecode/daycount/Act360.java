package com.examplecode.daycount;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Actual/360 day-count convention.
 *
 * <p>Counts the actual number of calendar days between two dates and divides
 * by a fixed 360-day year. Commonly used for money-market instruments and
 * floating-rate notes.</p>
 *
 * <p>Formula: {@code dayCount(start, end) / 360}</p>
 */
public class Act360 implements DayCountConvention {

    /**
     * Calculates the year fraction as actual days divided by 360.
     *
     * @param start the start date (inclusive)
     * @param end   the end date (exclusive)
     * @return year fraction
     * @throws IllegalArgumentException if {@code end} is before {@code start}
     */
    @Override
    public double yearFraction(LocalDate start, LocalDate end) {

        if (end.isBefore(start)){
            throw new IllegalArgumentException("End date must be after start date");
        }

        long actualDays = ChronoUnit.DAYS.between(start, end);
        return actualDays / 360.0;
    }

    /** @return {@code "ACT/360"} */
    @Override
    public String getName() {
        return "ACT/360";
    }
}
