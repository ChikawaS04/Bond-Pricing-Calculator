package com.fixedIncome.sensitivityanalysis;

import com.fixedIncome.pricer.FixedRateBondPricer;
import com.fixedIncome.securities.Bond;

/**
 * Computes the DV01 (Dollar Value of a 01) for a fixed-rate bond.
 *
 * <p>DV01 measures the change in a bond's price for a 1 basis point (0.01%)
 * decrease in yield. It is derived from modified duration:</p>
 *
 * <pre>
 *   DV01 = Modified Duration × Dirty Price × 0.0001
 * </pre>
 */
public class BondDV01 {

    /** Internal {@link BondMacaulayDuration} instance used when computing DV01 via modified duration. */
    BondMacaulayDuration bondMacaulayDurationSolved = new BondMacaulayDuration();

    /**
     * Calculates the DV01 for a bond at a given yield-to-maturity.
     *
     * @param bondModifiedDurationSolved the {@link BondModifiedDuration} instance used to compute modified duration
     * @param fixedRateBondPricer        the pricer used to compute the bond's dirty price
     * @param bond                       the bond for which DV01 is calculated
     * @param ytm                        the yield-to-maturity as a decimal (e.g. 0.05 for 5%)
     * @return the DV01 in dollars per 1 basis point move in yield
     */
    public double calculateDV01(BondModifiedDuration bondModifiedDurationSolved, FixedRateBondPricer fixedRateBondPricer, Bond bond, double ytm) {
        double modDur = bondModifiedDurationSolved.calculateModifiedDuration(bondMacaulayDurationSolved, bond, ytm);
        return modDur * fixedRateBondPricer.dirtyPrice(bond, ytm) * 0.0001;
    }
}
