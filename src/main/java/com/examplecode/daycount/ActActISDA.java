package com.examplecode.daycount;

import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;

public class ActActISDA implements DayCountConvention{

    @Override
    public double yearFraction(LocalDate start, LocalDate end) {
        if (!start.isBefore(end)) {
            return 0.0;
        }

        double yearFrac = 0.0;
        LocalDate current = start;

        while (current.isBefore(end)) {
            int year = current.getYear();
            LocalDate yearEnd = LocalDate.of(year + 1, 1, 1);
            LocalDate periodEnd = yearEnd.isBefore(end) ? yearEnd : end;

            long daysInPeriod = ChronoUnit.DAYS.between(current, periodEnd);
            int daysInYear = Year.isLeap(year) ? 366 : 365;

            yearFrac += (double) daysInPeriod / daysInYear;
            current = periodEnd;
        }

        return yearFrac;
    }

    @Override
    public String getName() {
        return "ACT/ACT ISDA";
    }
}
