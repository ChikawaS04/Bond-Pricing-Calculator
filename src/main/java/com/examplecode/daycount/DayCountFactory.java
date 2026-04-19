package com.examplecode.daycount;

import java.util.Map;

public class DayCountFactory {

    private static final Map<String, DayCountConvention> CONVENTIONS = Map.of(
            "ACT/360", new Act360(),
            "ACT/365", new Act365(),
            "30/360", new Thirty360US(),
            "ACT/ACT ISDA", new ActActISDA()
    );

    public static DayCountConvention getConvention(String name) {
        DayCountConvention convention = CONVENTIONS.get(name.toUpperCase());
        if(convention == null) {
            throw new IllegalArgumentException("Invalid day count convention: " + name);
        }
        return convention;
    }
}