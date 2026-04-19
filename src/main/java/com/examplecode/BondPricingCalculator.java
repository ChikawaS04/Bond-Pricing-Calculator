package com.examplecode;

import com.examplecode.daycount.DayCountConvention;
import com.examplecode.daycount.DayCountFactory;

import java.time.LocalDate;

public class BondPricingCalculator {
    static void main(String[] args) {

        DayCountConvention dcc = DayCountFactory.getConvention("ACT/365");

        Bond bond = new Bond(
                1000.0,
                0.05,
                5,
                2,
                LocalDate.of(2023, 1, 15),
                LocalDate.of(2028, 1, 15),
                "ACT/365"
        );

        BondPricer pricer = new BondPricer();
        LocalDate settlement = LocalDate.of(2024, 3, 20);

        double dirty = pricer.dirtyPrice(bond, settlement, 0.06);
        double ai    = pricer.accruedInterest(bond, settlement);
        double clean = pricer.cleanPrice(bond, settlement, 0.06);

        System.out.printf("Dirty: %.4f%n", dirty);
        System.out.printf("AI:    %.4f%n", ai);
        System.out.printf("Clean: %.4f%n", clean);
    }
}
