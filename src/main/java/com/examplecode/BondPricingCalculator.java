package com.examplecode;

public class BondPricingCalculator {
    static void main(String[] args) {
        Bond bond = new Bond(1000.0, 0.05, 10, 2);
        double pv = bond.calculatePresentValue(0.06);
        System.out.println(bond);
        System.out.printf("Present Value at 6%% yield: %.2f%n", pv);    }
}
