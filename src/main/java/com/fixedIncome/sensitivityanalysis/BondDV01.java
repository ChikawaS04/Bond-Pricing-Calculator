package com.fixedIncome.sensitivityanalysis;

import com.fixedIncome.pricer.FixedRateBondPricer;
import com.fixedIncome.securities.Bond;

public class BondDV01 {

    BondMacaulayDuration bondMacaulayDurationSolved = new BondMacaulayDuration();

    public double calculateDV01(BondModifiedDuration bondModifiedDurationSolved, FixedRateBondPricer fixedRateBondPricer, Bond bond, double ytm) {
        double modDur = bondModifiedDurationSolved.calculateModifiedDuration(bondMacaulayDurationSolved, bond, ytm);
        return modDur * fixedRateBondPricer.dirtyPrice(bond, ytm) * 0.0001;
    }
}
