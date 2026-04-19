package com.examplecode.daycount;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Act365 implements DayCountConvention {

    @Override
    public double yearFraction(LocalDate start, LocalDate end) {

        if (end.isBefore(start)){
            throw new IllegalArgumentException("End date must be after start date");
        }

        long actualDays = ChronoUnit.DAYS.between(start, end);
        return actualDays / 365.0;
    }

    @Override
    public String getName() {
        return "ACT/365";
    }
}
