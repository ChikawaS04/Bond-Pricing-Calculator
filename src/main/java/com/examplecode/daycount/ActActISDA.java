package com.examplecode.daycount;

import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;

/**
 * Actual/Actual ISDA day-count convention.
 *
 * <p>Divides each sub-period within a calendar year by the actual number of days
 * in that year (365 or 366 for leap years), then sums the results. This correctly
 * handles date ranges that span a year boundary.</p>
 *
 * <p>Used for US Treasury bonds and many government securities globally.</p>
 *
 * <p>Formula (per ISDA 2006 definitions):</p>
 * <pre>
 *   sum over each calendar year y spanned:
 *     daysInY(start, end) / daysInYear(y)
 * </pre>
 */
public class ActActISDA implements DayCountConvention {

    /**
     * Calculates the year fraction by accumulating sub-period fractions year by year.
     *
     * <p>Returns {@code 0.0} if {@code start} is not before {@code end}.</p>
     *
     * @param start the start date (inclusive)
     * @param end   the end date (exclusive)
     * @return year fraction
     */
    @Override
    public double yearFraction(LocalDate start, LocalDate end) {
        if (!start.isBefore(end)) {
            return 0.0;
        }

        double yearFrac = 0.0;
        LocalDate current = start;

        while (current.isBefore(end)) {
            int year = current.getYear();
            LocalDate yearEnd = LocalDate.of(year + 1, 1, 1); // first day of next year
            LocalDate periodEnd = yearEnd.isBefore(end) ? yearEnd : end;

            long daysInPeriod = ChronoUnit.DAYS.between(current, periodEnd);
            int daysInYear = Year.isLeap(year) ? 366 : 365;

            yearFrac += (double) daysInPeriod / daysInYear;
            current = periodEnd;
        }

        return yearFrac;
    }

    /** @return {@code "ACT/ACT ISDA"} */
    @Override
    public String getName() {
        return "ACT/ACT ISDA";
    }
}
