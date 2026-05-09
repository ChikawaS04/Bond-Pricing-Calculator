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
 *   <li>{@code "ACT/ACT ICMA"} — requires payment frequency; use
 *       {@link #getConvention(String, int)} for correct results</li>
 * </ul>
 */
public class DayCountFactory {

    /** Immutable map of frequency-independent conventions, keyed by upper-case name. */
    private static final Map<String, DayCountConvention> CONVENTIONS = Map.of(
            "ACT/360", new Act360(),
            "ACT/365", new Act365(),
            "30/360", new Thirty360US(),
            "ACT/ACT ISDA", new ActActISDA(),
            "ACT/ACT ICMA", new ActActICMA(2)   // default semi-annual; use getConvention(name, freq) for other frequencies
    );

    /**
     * Returns the {@link DayCountConvention} for the given name.
     *
     * <p>For {@code "ACT/ACT ICMA"} this returns a default semi-annual (frequency=2)
     * instance. Use {@link #getConvention(String, int)} when the bond's payment
     * frequency is known.</p>
     *
     * @param name convention name (case-insensitive, e.g. {@code "ACT/365"})
     * @return the matching convention
     * @throws IllegalArgumentException if the name is not recognised
     */
    public static DayCountConvention getConvention(String name) {
        DayCountConvention convention = CONVENTIONS.get(name.toUpperCase());
        if (convention == null) {
            throw new IllegalArgumentException("Invalid day count convention: " + name);
        }
        return convention;
    }

    /**
     * Returns the {@link DayCountConvention} for the given name, using {@code frequency}
     * for conventions that require it.
     *
     * <p>For {@code "ACT/ACT ICMA"}, a correctly configured instance is returned for the
     * supplied frequency. For all other conventions the frequency parameter is ignored
     * and the behaviour is identical to {@link #getConvention(String)}.</p>
     *
     * @param name      convention name (case-insensitive)
     * @param frequency coupon payments per year (used only for {@code "ACT/ACT ICMA"})
     * @return the matching convention
     * @throws IllegalArgumentException if the name is not recognised
     */
    public static DayCountConvention getConvention(String name, int frequency) {
        if (name.toUpperCase().equals("ACT/ACT ICMA")) {
            return new ActActICMA(frequency);
        }
        return getConvention(name);
    }
}
