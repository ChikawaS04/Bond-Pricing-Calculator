package com.fixedIncome.daycount;

import java.time.LocalDate;

/**
 * 30/360 US (Bond Basis) day-count convention.
 *
 * <p>Assumes each month has exactly 30 days and each year has exactly 360 days.
 * Day adjustments follow the US (NASD) rules:</p>
 * <ul>
 *   <li>If {@code d1 == 31}, set {@code d1 = 30}.</li>
 *   <li>If {@code d2 == 31} and {@code d1 >= 30}, set {@code d2 = 30}.</li>
 * </ul>
 *
 * <p>Widely used for US corporate and agency bonds.</p>
 *
 * <p>Formula: {@code [360*(y2-y1) + 30*(m2-m1) + (d2-d1)] / 360}</p>
 */
public class Thirty360US implements DayCountConvention {

    /**
     * Calculates the year fraction using the 30/360 US day-count rules.
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

        int d1 = start.getDayOfMonth();
        int d2 = end.getDayOfMonth();
        int m1 = start.getMonthValue();
        int m2 = end.getMonthValue();
        int y1 = start.getYear();
        int y2 = end.getYear();

        // US (NASD) end-of-month adjustments
        if (d1 == 31) {
            d1 = 30;
        }
        if (d2 == 31 && d1 >= 30) {
            d2 = 30;
        }

        int days = 360 * (y2 - y1) + 30 * (m2 - m1) + (d2 - d1);
        return days / 360.0;
    }

    /** @return {@code "30/360 US"} */
    @Override
    public String getName() {
        return "30/360 US";
    }
}
