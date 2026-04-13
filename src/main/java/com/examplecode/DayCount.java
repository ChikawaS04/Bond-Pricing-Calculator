package com.examplecode;

import java.time.LocalDate;

public interface DayCount {
    /**
     * Calculates the year fraction between two dates using a reference period.
     *
     * @param startDate The start date of the period.
     * @param endDate The end date of the period.
     * @param refStart The start date of the reference period.
     * @param refEnd The end date of the reference period.
     * @return The year fraction between startDate and endDate relative to the reference period.
     */
    double yearFraction(LocalDate startDate, LocalDate endDate, LocalDate refStart, LocalDate refEnd);
}
