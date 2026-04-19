package com.examplecode;

import com.examplecode.daycount.DayCountConvention;
import com.examplecode.daycount.DayCountFactory;

import java.time.LocalDate;
import java.util.List;

public class BondPricer {

    private static final double TOLERANCE = 1e-10;
    private static final int MAX_ITERATIONS = 100;

    //Accrued Interest Calculation
    public double accruedInterest(Bond bond, LocalDate settlementDate) {
        DayCountConvention dcc = DayCountFactory.getConvention(bond.getDayCountConvention());

        LocalDate lastCoupon = findLastCouponDate(bond, settlementDate);
        LocalDate nextCoupon = findNextCouponDate(bond, settlementDate);

        double fraction = dcc.yearFraction(lastCoupon, settlementDate) / dcc.yearFraction(lastCoupon, nextCoupon);
        return bond.getCouponPayment() * fraction;
    }

    //Dirty price - settlement-aware Present Value of all future cash flows
    public double dirtyPrice(Bond bond, LocalDate settlementDate, double ytm) {
        DayCountConvention dcc = DayCountFactory.getConvention(bond.getDayCountConvention());

        List<LocalDate> couponDates = bond.getCouponPaymentDates();
        double periodRate = ytm / bond.getPaymentFrequency();
        double dirtyPice = 0.0;

        // The fractional period from settlement to the next coupon date
        // is the exponent for the first cash flow. Every subsequent
        // cash flow adds exactly 1.0 to that exponent.
        LocalDate nextCoupon = findNextCouponDate(bond, settlementDate);
        LocalDate prevCoupon = findLastCouponDate(bond, settlementDate);

        double w = dcc.yearFraction(settlementDate, nextCoupon) /  dcc.yearFraction(prevCoupon, nextCoupon);

        int i =  0;
        for (LocalDate couponDate : couponDates) {
            if (!couponDate.isAfter(settlementDate)) continue;
            double exponent = w + i;
            double pv = bond.getCouponPayment() / Math.pow(1+ periodRate, exponent);
            dirtyPice += pv;

            // Add principal on the final cash flow
            if (couponDate.equals(bond.getMaturityDate())) {
                dirtyPice += bond.getFaceValue() / Math.pow(1+ periodRate, exponent);
            }

            i++;
        }

        return dirtyPice;
    }

    //Clean price
    public double cleanPrice(Bond bond, LocalDate settlementDate, double ytm) {
        return dirtyPrice(bond, settlementDate, ytm) - accruedInterest(bond, settlementDate);
    }

    //Private Helpers - Coupon Date Navigation
    private LocalDate findLastCouponDate(Bond bond, LocalDate settlementDate) {
        LocalDate last = bond.getIssueDate();
        for (LocalDate d : bond.getCouponPaymentDates()) {
            if (!d.isAfter(settlementDate)) last = d;
            else break;
        }
        return last;
    }

    private LocalDate findNextCouponDate(Bond bond, LocalDate settlementDate) {
        for (LocalDate d : bond.getCouponPaymentDates()) {
            if (d.isAfter(settlementDate)) return d;
        }
        return bond.getMaturityDate();
    }
}
