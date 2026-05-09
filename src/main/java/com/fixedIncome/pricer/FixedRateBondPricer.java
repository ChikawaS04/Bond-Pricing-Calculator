package com.fixedIncome.pricer;

import com.fixedIncome.securities.Bond;
import com.fixedIncome.daycount.DayCountConvention;
import com.fixedIncome.daycount.DayCountFactory;
import java.time.LocalDate;
import java.util.List;

public class FixedRateBondPricer {

    /** Convergence threshold for YTM Newton-Raphson iteration (price difference). */
    private static final double TOLERANCE = 1e-10;

    /** Maximum number of Newton-Raphson iterations before returning the current estimate. */
    private static final int MAX_ITERATIONS = 100;

    /**
     * Calculates accrued interest from the last coupon date up to (but not including)
     * the settlement date.
     *
     * @param bond the bond whose coupon, settlement date, and day-count convention are used
     * @return accrued interest as a dollar amount (not a fraction of face value)
     */
    public double accruedInterest(Bond bond) {
        if (!bond.getSettlementDate().isAfter(bond.getIssueDate())) {
            return 0.0;
        }

        DayCountConvention dcc = DayCountFactory.getConvention(bond.getDayCountConvention(), bond.getPaymentFrequency());

        LocalDate lastCoupon = findLastCouponDate(bond);
        LocalDate nextCoupon = findNextCouponDate(bond);

        double fraction = dcc.yearFraction(lastCoupon, bond.getSettlementDate())
                / dcc.yearFraction(lastCoupon, nextCoupon);
        return bond.getCouponPayment() * fraction;
    }

    /**
     * Calculates the dirty price (full price) of the bond: the present value of all future
     * cash flows discounted from the settlement date using the recomputed yield-to-maturity.
     *
     * <p>Uses fractional-period discounting so that settlement need not fall on a coupon date.
     * The first cash flow is discounted by {@code w} periods, where {@code w} is the fraction
     * of the current coupon period remaining as of settlement; each subsequent cash flow adds
     * one whole period to the exponent.</p>
     *
     * <p>The {@code ytm} parameter must be the recomputed yield returned by {@link #solveYTM},
     * not the vendor quoted yield from the bond.</p>
     *
     * @param bond the bond to price
     * @param ytm  recomputed annual yield-to-maturity (e.g. 0.05 for 5%)
     * @return dirty price (includes accrued interest)
     */
    public double dirtyPrice(Bond bond, double ytm) {
        DayCountConvention dcc = DayCountFactory.getConvention(bond.getDayCountConvention(), bond.getPaymentFrequency());
        List<LocalDate> couponDates = bond.getCouponPaymentDates();

        double periodRate = ytm / bond.getPaymentFrequency();
        double price = 0.0;

        LocalDate nextCoupon = findNextCouponDate(bond);
        LocalDate prevCoupon = findLastCouponDate(bond);

        // w = fraction of the current coupon period remaining as of settlement
        double w = dcc.yearFraction(bond.getSettlementDate(), nextCoupon)
                / dcc.yearFraction(prevCoupon, nextCoupon);

        // i counts only future coupon periods, starting at 0 for the next coupon
        int i = 0;
        for (LocalDate couponDate : couponDates) {
            if (!couponDate.isAfter(bond.getSettlementDate())) continue;

            double exponent = w + i++;
            double discountFactor = Math.pow(1 + periodRate, exponent);
            price += bond.getCouponPayment() / discountFactor;

            // In the final period, also discount the principal
            if (couponDate.equals(bond.getMaturityDate())) {
                price += bond.getFaceValue() / discountFactor;
            }
        }

        return price;
    }

    /**
     * Calculates the clean price of the bond: dirty price minus accrued interest.
     *
     * <p>Clean price is the conventionally quoted market price and does not include
     * interest that has accrued since the last coupon payment.</p>
     *
     * <p>The {@code ytm} parameter must be the recomputed yield returned by {@link #solveYTM},
     * not the vendor quoted yield from the bond.</p>
     *
     * @param bond the bond to price
     * @param ytm  recomputed annual yield-to-maturity (e.g. 0.05 for 5%)
     * @return clean price (excludes accrued interest)
     */
    public double cleanPrice(Bond bond, double ytm) {
        return dirtyPrice(bond, ytm) - accruedInterest(bond);
    }

    /**
     * Solves for the yield-to-maturity (YTM) that reproduces the given dirty price,
     * using Newton-Raphson iteration with an analytic derivative.
     *
     * <p>The analytic derivative dP/dy is computed in the same pass as the price,
     * eliminating the need for numerical differentiation. The derivative of each
     * discounted cash flow CF / (1 + y/m)^(w+i) with respect to y is:
     * -(w+i)/m * CF / (1 + y/m)^(w+i+1).</p>
     *
     * <p>The solver is seeded with {@code bond.getQuotedYield()} as the initial guess,
     * placing iteration in the right neighborhood immediately for most bonds.</p>
     *
     * <p>Iteration stops when the price error falls within {@code TOLERANCE} or after
     * {@code MAX_ITERATIONS} steps.</p>
     *
     * @param bond             the bond to solve
     * @param targetDirtyPrice the observed market dirty price to match
     * @return recomputed YTM (annual, e.g. 0.05 for 5%)
     * @throws ArithmeticException if the derivative collapses to near-zero or the estimate diverges
     */
    public double solveYTM(Bond bond, double targetDirtyPrice) {
        DayCountConvention dcc = DayCountFactory.getConvention(bond.getDayCountConvention(), bond.getPaymentFrequency());
        List<LocalDate> couponDates = bond.getCouponPaymentDates();

        LocalDate nextCoupon = findNextCouponDate(bond);
        LocalDate prevCoupon = findLastCouponDate(bond);

        // Precompute w once -- it does not change across Newton-Raphson iterations
        double w = dcc.yearFraction(bond.getSettlementDate(), nextCoupon)
                / dcc.yearFraction(prevCoupon, nextCoupon);

        // Seed with the vendor-quoted yield -- already close to the solution for most bonds
        double ytm = bond.getQuotedYield();

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            double periodRate = ytm / bond.getPaymentFrequency();
            double price = 0.0;
            double dPdy  = 0.0;

            // i counts only future coupon periods, starting at 0 for the next coupon
            int i = 0;
            for (LocalDate couponDate : couponDates) {
                if (!couponDate.isAfter(bond.getSettlementDate())) continue;

                double exponent       = w + i++;
                double discountFactor = Math.pow(1 + periodRate, exponent);
                double couponPV       = bond.getCouponPayment() / discountFactor;

                price += couponPV;
                // d/dy [C / (1 + y/m)^(w+i)] = -(w+i)/m * C / (1 + y/m)^(w+i+1)
                dPdy  -= (exponent / bond.getPaymentFrequency()) * couponPV / (1 + periodRate);

                if (couponDate.equals(bond.getMaturityDate())) {
                    double principalPV = bond.getFaceValue() / discountFactor;
                    price += principalPV;
                    dPdy  -= (exponent / bond.getPaymentFrequency()) * principalPV / (1 + periodRate);
                }
            }

            double diff = price - targetDirtyPrice;
            if (Math.abs(diff) < TOLERANCE) break;

            if (Math.abs(dPdy) < 1e-12) {
                throw new ArithmeticException(
                        "YTM solver: derivative collapsed to near-zero at ytm=" + ytm + "; cannot converge.");
            }

            ytm -= diff / dPdy;

            if (ytm < -0.999 || ytm > 100.0) {
                throw new ArithmeticException(
                        "YTM solver: estimate diverged to " + ytm + "; check input price and bond cash flows.");
            }
        }

        return ytm;
    }

    /**
     * Returns the most recent coupon date on or before the settlement date.
     * Falls back to the issue date if the settlement precedes the first coupon.
     */
    public LocalDate findLastCouponDate(Bond bond) {
        LocalDate last = bond.getIssueDate();
        for (LocalDate d : bond.getCouponPaymentDates()) {
            if (!d.isAfter(bond.getSettlementDate())) last = d;
            else break;
        }
        return last;
    }

    /**
     * Returns the first coupon date strictly after the settlement date.
     * Falls back to the maturity date if no future coupon exists.
     */
    public LocalDate findNextCouponDate(Bond bond) {
        for (LocalDate d : bond.getCouponPaymentDates()) {
            if (d.isAfter(bond.getSettlementDate())) return d;
        }
        return bond.getMaturityDate();
    }
}