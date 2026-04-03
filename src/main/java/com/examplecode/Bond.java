package com.examplecode;

public class Bond {

    private final double faceValue;
    private final double couponRate;
    private final int yearsToMaturity;
    private final int paymentFrequency;

    public double getFaceValue() {
        return faceValue;
    }

    public double getCouponRate() {
        return couponRate;
    }

    public int getYearsToMaturity() {
        return yearsToMaturity;
    }

    public int getPaymentFrequency() {
        return paymentFrequency;
    }

    public Bond (double faceValue, double couponRate, int yearsToMaturity, int paymentFrequency) {
        this.faceValue = faceValue;
        this.couponRate = couponRate / 100;
        this.yearsToMaturity = yearsToMaturity;
        this.paymentFrequency = paymentFrequency;

        if (faceValue < 0 || couponRate < 0) {
            throw new IllegalArgumentException("Face value AND Coupon rate must be greater than zero.");
        }
        if (yearsToMaturity <= 0 || paymentFrequency < 0) {
            throw new IllegalArgumentException("Years to maturity AND Payment frequency must be greater than zero.");
        }
    }

    public double getCouponPayment () {
        return getFaceValue() * getCouponRate() / getPaymentFrequency();
    }

    public double calculatePresentValue (double discountRate) {

    }
}
