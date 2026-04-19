package com.examplecode;

import com.examplecode.daycount.DayCountConvention;
import com.examplecode.daycount.DayCountFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Bond {

    private final double faceValue;
    private final double couponRate;
    private final int yearsToMaturity;
    private final int paymentFrequency;
    private final LocalDate issueDate;
    private final LocalDate maturityDate;
    private final String dayCountConvention;

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
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getMaturityDate() { return maturityDate; }
    public String getDayCountConvention() { return dayCountConvention; }

    public Bond (double faceValue, double couponRate, int yearsToMaturity, int paymentFrequency, LocalDate issueDate, LocalDate maturityDate, String dayCountConvention) {

        if (faceValue <= 0 || couponRate <= 0) {
            throw new IllegalArgumentException("Face value AND Coupon rate must be greater than zero.");
        }
        if (yearsToMaturity <= 0 || paymentFrequency <= 0) {
            throw new IllegalArgumentException("Years to maturity AND Payment frequency must be greater than zero.");
        }
        if (issueDate == null || maturityDate == null) {
            throw new IllegalArgumentException("Issue date and maturity date are required.");
        }
        if (!maturityDate.isAfter(issueDate)) {
            throw new IllegalArgumentException("Maturity date must be after issue date.");
        }
        if (dayCountConvention == null) {
            throw new IllegalArgumentException("Day count convention is required.");
        }

        DayCountFactory.getConvention(dayCountConvention);

        this.faceValue = faceValue;
        this.couponRate = couponRate;
        this.yearsToMaturity = yearsToMaturity;
        this.paymentFrequency = paymentFrequency;
        this.issueDate = issueDate;
        this.maturityDate = maturityDate;
        this.dayCountConvention = dayCountConvention.toUpperCase();
    }

    public double getCouponPayment () {
        return getFaceValue() * getCouponRate() / getPaymentFrequency(); //C = c * F / m
    }

    public List<LocalDate> getCouponPaymentDates() {
        List<LocalDate> dates = new ArrayList<>();
        int monthsPeriod = 12 / paymentFrequency;
        LocalDate date = issueDate.plusMonths(monthsPeriod);
        while (!date.isAfter(maturityDate)) {
            dates.add(date);
            date = date.plusMonths(monthsPeriod);
        }
        return dates;
    }

    /*
    public double calculatePresentValue (double annualDiscountRate) {
        double periodicRate = annualDiscountRate / getPaymentFrequency(); //r = YTM / m
        int totalPeriods = getYearsToMaturity() * getPaymentFrequency(); //n = years to maturity * payment frequency

        //PV = C × [1 - (1 + r)^(-n)] / r + F / (1 + r)^n
        double pvCoupons = getCouponPayment() * (1 - Math.pow(1 + periodicRate, -totalPeriods)) / periodicRate;
        double pvPrincipal = getFaceValue() / Math.pow(1 + periodicRate, totalPeriods);

        return pvCoupons + pvPrincipal;
    }
     */

    @Override
    public String toString () {
        return "Bond[faceValue=" + getFaceValue() + ", couponRate=" + getCouponRate() + ", maturity=" + getYearsToMaturity() + "yrs" + ", frequency=" + getPaymentFrequency() + "]";
    }
}
