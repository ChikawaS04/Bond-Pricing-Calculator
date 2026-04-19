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
        if (faceValue <= 0 || couponRate <= 0) {
            throw new IllegalArgumentException("Face value AND Coupon rate must be greater than zero.");
        }
        if (yearsToMaturity <= 0 || paymentFrequency <= 0) {
            throw new IllegalArgumentException("Years to maturity AND Payment frequency must be greater than zero.");
        }

        this.faceValue = faceValue;
        this.couponRate = couponRate;
        this.yearsToMaturity = yearsToMaturity;
        this.paymentFrequency = paymentFrequency;
    }

    public double getCouponPayment () {
        return getFaceValue() * getCouponRate() / getPaymentFrequency();
    }

    public double calculatePresentValue (double annualDiscountRate) {
        double periodicRate = annualDiscountRate / getPaymentFrequency(); //r
        int totalPeriods = getYearsToMaturity() * getPaymentFrequency(); //n

        double pvCoupons = getCouponPayment() * (1 - Math.pow(1 + periodicRate, -totalPeriods)) / periodicRate;
        double pvPrincipal = getFaceValue() / Math.pow(1 + periodicRate, totalPeriods);

        return pvCoupons + pvPrincipal;
    }

    @Override
    public String toString () {
        return "Bond[faceValue=" + getFaceValue() + ", couponRate=" + getCouponRate() + ", maturity=" + getYearsToMaturity() + "yrs" + ", frequency=" + getPaymentFrequency() + "]";
    }
}
