package com.examplecode;

import com.examplecode.daycount.DayCountConvention;
import com.examplecode.daycount.DayCountFactory;
import java.time.LocalDate;
import java.util.List;

public class BondPricer {

    /** Convergence threshold for YTM Newton-Raphson iteration (price difference). */
    private static final double TOLERANCE = 1e-10;

    /** Maximum number of Newton-Raphson iterations before returning the current estimate. */
    private static final int MAX_ITERATIONS = 100;

    /**
     * Calculates accrued interest from the last coupon date up to (but not including) the settlement date.
     *
     * @param bond           the bond whose coupon and day-count convention are used
     * @param settlementDate the trade settlement date
     * @return accrued interest as a dollar amount (not a fraction of face value)
     */
    public double accruedInterest(Bond bond, LocalDate settlementDate) {
        if (!settlementDate.isAfter(bond.getIssueDate())) {
            return 0.0;
        }

        DayCountConvention dcc = DayCountFactory.getConvention(bond.getDayCountConvention());

        LocalDate lastCoupon = findLastCouponDate(bond, settlementDate);
        LocalDate nextCoupon = findNextCouponDate(bond, settlementDate);

        double fraction = dcc.yearFraction(lastCoupon, settlementDate) / dcc.yearFraction(lastCoupon, nextCoupon);
        return bond.getCouponPayment() * fraction;
    }

    /**
     * Calculates the dirty price (full price) of the bond: the present value of all future
     * cash flows discounted from the settlement date using the given yield-to-maturity.
     *
     * <p>Uses fractional-period discounting so that settlement need not fall on a coupon date.
     * The first cash flow is discounted by {@code w} periods, where {@code w} is the fraction
     * of the current coupon period remaining as of settlement; each subsequent cash flow adds
     * one whole period to the exponent.</p>
     *
     * @param bond           the bond to price
     * @param settlementDate the trade settlement date
     * @param ytm            annual yield-to-maturity (e.g. 0.05 for 5%)
     * @return dirty price (includes accrued interest)
     */
    public double dirtyPrice(Bond bond, LocalDate settlementDate, double ytm) {
        DayCountConvention dcc = DayCountFactory.getConvention(bond.getDayCountConvention());

        List<LocalDate> couponDates = bond.getCouponPaymentDates();

        // Period rate formula, using: r = YTM / m
        double periodRate = ytm / bond.getPaymentFrequency();
        double dirtyPice = 0.0;

        // The fractional period from settlement to the next coupon date
        // is the exponent for the first cash flow. Every subsequent
        // cash flow adds exactly 1.0 to that exponent.
        LocalDate nextCoupon = findNextCouponDate(bond, settlementDate);
        LocalDate prevCoupon = findLastCouponDate(bond, settlementDate);

        // Using w = yearFraction(settle, next) / yearFraction(prev, next)
        double w = dcc.yearFraction(settlementDate, nextCoupon) /  dcc.yearFraction(prevCoupon, nextCoupon);

        int i =  0;
        for (LocalDate couponDate : couponDates) {
            if (!couponDate.isAfter(settlementDate)) continue;
            double exponent = w + i; // fractional first period + whole periods thereafter

            // Coupon PV for this period: C / (1 + r)^(w+i)
            double pv = bond.getCouponPayment() / Math.pow(1+ periodRate, exponent);
            dirtyPice += pv;

            // In the final period, also discount the principal: F / (1 + r)^(w+(N-1))
            if (couponDate.equals(bond.getMaturityDate())) {
                dirtyPice += bond.getFaceValue() / Math.pow(1+ periodRate, exponent);
            }

            i++;
        }

        return dirtyPice;
    }

    /**
     * Calculates the clean price of the bond: dirty price minus accrued interest.
     *
     * <p>Clean price is the conventionally quoted market price and does not include
     * interest that has accrued since the last coupon payment.</p>
     *
     * @param bond           the bond to price
     * @param settlementDate the trade settlement date
     * @param ytm            annual yield-to-maturity (e.g. 0.05 for 5%)
     * @return clean price (excludes accrued interest)
     */
    public double cleanPrice(Bond bond, LocalDate settlementDate, double ytm) {
        return dirtyPrice(bond, settlementDate, ytm) - accruedInterest(bond, settlementDate);
    }

    /**
     * Solves for the yield-to-maturity (YTM) that produces the given dirty price,
     * using Newton-Raphson iteration with a central-difference numerical derivative.
     * Iteration stops when the price error falls within {@code TOLERANCE} or after
     * {@code MAX_ITERATIONS} steps.
     *
     * @param bond              the bond to solve
     * @param settlementDate    the trade settlement date
     * @param targetDirtyPrice  the observed market dirty price to match
     * @return estimated YTM (annual, e.g. 0.05 for 5%)
     */
    public double solveYTM(Bond bond, LocalDate settlementDate, double targetDirtyPrice) {
        double ytm = 0.05; // initial guess: 5%

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double price = dirtyPrice(bond, settlementDate, ytm);
            double diff = price - targetDirtyPrice;

            if (Math.abs(diff) < TOLERANCE) break; // converged

            // Numerical derivative: dP/dy ≈ (P(y+h) - P(y-h)) / 2h
            double h = 1e-5;
            double dPdy = (dirtyPrice(bond, settlementDate, ytm + h) - dirtyPrice(bond, settlementDate, ytm - h)) / (2 * h);

            // Newton-Raphson step: y_new = y - f(y) / f'(y)
            ytm -= diff / dPdy;
        }

        return ytm;
    }



    /**
     * Returns the most recent coupon date on or before the settlement date.
     * Falls back to the issue date if the settlement precedes the first coupon.
     */
    private LocalDate findLastCouponDate(Bond bond, LocalDate settlementDate) {
        LocalDate last = bond.getIssueDate();
        for (LocalDate d : bond.getCouponPaymentDates()) {
            if (!d.isAfter(settlementDate)) last = d;
            else break;
        }
        return last;
    }

    /**
     * Returns the first coupon date strictly after the settlement date.
     * Falls back to the maturity date if no future coupon exists.
     */
    private LocalDate findNextCouponDate(Bond bond, LocalDate settlementDate) {
        for (LocalDate d : bond.getCouponPaymentDates()) {
            if (d.isAfter(settlementDate)) return d;
        }
        return bond.getMaturityDate();
    }
}