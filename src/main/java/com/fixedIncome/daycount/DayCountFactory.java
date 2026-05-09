package com.fixedIncome.daycount;

import java.util.Map;

/**
 * Factory for resolving {@link DayCountConvention} instances by name.
 *
 * <p>Supported convention strings (case-insensitive):</p>
 * <ul>
 *   <li>{@code "ACT/360"}</li>
 *   <li>{@code "ACT/365"}</li>
 *   <li>{@code "30/360"}</li>
 *   <li>{@code "ACT/ACT ISDA"}</li>
 * </ul>
 */
public class DayCountFactory {

    /** Immutable map of all supported conventions, keyed by upper-case name. */
    private static final Map<String, DayCountConvention> CONVENTIONS = Map.of(
            "ACT/360", new Act360(),
            "ACT/365", new Act365(),
            "30/360", new Thirty360US(),
            "ACT/ACT ISDA", new ActActISDA()
    );

    /**
     * Returns the {@link DayCountConvention} for the given name.
     *
     * @param name convention name (case-insensitive, e.g. {@code "ACT/365"})
     * @return the matching convention
     * @throws IllegalArgumentException if the name is not recognised
     */
    public static DayCountConvention getConvention(String name) {
        DayCountConvention convention = CONVENTIONS.get(name.toUpperCase());
        if(convention == null) {
            throw new IllegalArgumentException("Invalid day count convention: " + name);
        }
        return convention;
    }
}
