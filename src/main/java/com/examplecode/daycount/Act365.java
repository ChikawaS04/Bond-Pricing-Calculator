package com.examplecode.daycount;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Actual/365 Fixed day-count convention.
 *
 * <p>Counts the actual number of calendar days between two dates and divides
 * by a fixed 365-day year (leap years are ignored). Commonly used for UK
 * government bonds (gilts) and some corporate bonds.</p>
 *
 * <p>Formula: {@code dayCount(start, end) / 365}</p>
 */
public class Act365 implements DayCountConvention {

    /**
     * Calculates the year fraction as actual days divided by 365.
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
        return actualDays / 365.0;
    }

    /** @return {@code "ACT/365"} */
    @Override
    public String getName() {
        return "ACT/365";
    }
}
