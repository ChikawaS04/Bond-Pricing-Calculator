package com.fixedIncome.sensitivityanalysis;

import com.fixedIncome.securities.Bond;

/**
 * Computes the modified duration of a fixed-rate bond.
 *
 * <p>Modified duration measures the percentage change in a bond's price
 * for a 1% change in yield. It is derived from Macaulay duration:</p>
 *
 * <pre>
 *   Modified Duration = Macaulay Duration / (1 + YTM / Payment Frequency)
 * </pre>
 */
public class BondModifiedDuration {

    /**
     * Calculates the modified duration for a bond at a given yield-to-maturity.
     *
     * @param macaulayDurationSolved the {@link BondMacaulayDuration} instance used to compute Macaulay duration
     * @param bond                   the bond for which modified duration is calculated
     * @param ytm                    the yield-to-maturity as a decimal (e.g. 0.05 for 5%)
     * @return the modified duration in years
     */
    public double calculateModifiedDuration(BondMacaulayDuration macaulayDurationSolved, Bond bond, double ytm) {
        return macaulayDurationSolved.calculateMacaulayDuration(bond, ytm) / (1 + ytm / bond.getPaymentFrequency());
    }
}
