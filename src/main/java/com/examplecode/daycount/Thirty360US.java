package com.examplecode.daycount;

import java.time.LocalDate;

public class Thirty360US implements DayCountConvention {

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

        if (d1 == 31) {
            d1 = 30;
        }
        if (d2 == 31 && d1 >= 30) {
            d2 = 30;
        }

        int days = 360 * (y2 -y1) + 30 * (m2 - m1) + (d2 - d1);
        return days / 360.0;
    }

    @Override
    public String getName() {
        return "30/360 US";
    }
}
