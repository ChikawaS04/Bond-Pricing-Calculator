package com.examplecode.daycount;

import java.time.LocalDate;

/**
 * Contract for day-count convention implementations used in bond pricing.
 *
 * <p>A day-count convention defines how the number of days between two dates is
 * counted and how that count is converted to a year fraction. Different conventions
 * are standard across bond markets (e.g. government vs. corporate vs. money market).</p>
 */
public interface DayCountConvention {

    /**
     * Calculates the year fraction between two dates under this convention.
     *
     * @param start the start date (inclusive)
     * @param end   the end date (exclusive)
     * @return year fraction as a decimal (e.g. 0.5 for roughly half a year)
     */
    double yearFraction(LocalDate start, LocalDate end);

    /**
     * Returns the canonical name of this convention (e.g. {@code "ACT/365"}).
     *
     * @return convention name string
     */
    String getName();
}
