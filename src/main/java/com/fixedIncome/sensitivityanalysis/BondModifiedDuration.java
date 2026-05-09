package com.fixedIncome.sensitivityanalysis;

import com.fixedIncome.securities.Bond;

public class BondModifiedDuration {

    public double calculateModifiedDuration(BondMacaulayDuration macaulayDurationSolved, Bond bond, double ytm) {
        return macaulayDurationSolved.calculateMacaulayDuration(bond, ytm) / (1 + ytm / bond.getPaymentFrequency());
    }
}
