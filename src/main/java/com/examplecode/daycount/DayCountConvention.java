package com.examplecode.daycount;

import java.time.LocalDate;

public interface DayCountConvention {
    double yearFraction(LocalDate start, LocalDate end);
    String getName();
}
