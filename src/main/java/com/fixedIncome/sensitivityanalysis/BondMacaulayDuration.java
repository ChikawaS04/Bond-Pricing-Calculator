package com.fixedIncome.sensitivityanalysis;

import com.fixedIncome.securities.Bond;
import com.fixedIncome.pricer.FixedRateBondPricer;
import com.fixedIncome.daycount.DayCountConvention;
import com.fixedIncome.daycount.DayCountFactory;

import java.time.LocalDate;
import java.util.List;

/**
 * Computes the Macaulay duration of a fixed-rate bond.
 *
 * <p>Macaulay duration is the present-value-weighted average time (in years) to
 * receive each cash flow. It is defined as:</p>
 *
 * <pre>
 *   D = (1/P) * Σ [ t_i * CF_i / (1 + y/m)^(t_i * m) ]
 * </pre>
 *
 * <p>where {@code P} is the dirty price, {@code CF_i} is the cash flow at time
 * {@code t_i} (in years), {@code y} is the annual yield-to-maturity, and {@code m}
 * is the payment frequency.</p>
 *
 * <p>Fractional-period discounting is used so that settlement need not fall on a
 * coupon date — the first cash flow is discounted by {@code w} periods, where
 * {@code w} is the fraction of the current coupon period remaining as of settlement.</p>
 */
public class BondMacaulayDuration {

    /** Pricer used for dirty price computation and coupon-window lookups. */
    FixedRateBondPricer pricer = new FixedRateBondPricer();

    /**
     * Calculates the Macaulay duration of the bond in years.
     *
     * <p>Uses the same fractional first-period offset ({@code w}) as
     * {@code FixedRateBondPricer.dirtyPrice}, ensuring consistent discounting.
     * Time to each cash flow is {@code (w + i) / paymentFrequency} years, where
     * {@code i} is the zero-based index over future coupon dates.</p>
     *
     * @param bond the bond to evaluate; must have at least one future coupon date
     *             after the settlement date
     * @param ytm  annual yield-to-maturity (e.g. 0.05 for 5%), typically the
     *             recomputed yield from {@code FixedRateBondPricer.solveYTM}
     * @return Macaulay duration in years
     */
    public double calculateMacaulayDuration(Bond bond, double ytm) {
        DayCountConvention dcc = DayCountFactory.getConvention(bond.getDayCountConvention(), bond.getPaymentFrequency());
        List<LocalDate> couponDates = bond.getCouponPaymentDates();

        LocalDate nextCoupon = pricer.findNextCouponDate(bond);
        LocalDate prevCoupon = pricer.findLastCouponDate(bond);

        // Same fractional-period offset used in dirtyPrice/solveYTM
        double w = dcc.yearFraction(bond.getSettlementDate(), nextCoupon)
                / dcc.yearFraction(prevCoupon, nextCoupon);

        double periodRate = ytm / bond.getPaymentFrequency();
        double weightedPVSum = 0.0;

        int i = 0;
        for (LocalDate couponDate : couponDates) {
            if (!couponDate.isAfter(bond.getSettlementDate())) continue;

            double exponent   = w + i++;
            double timeInYears = exponent / bond.getPaymentFrequency();

            double cashFlow = bond.getCouponPayment();
            if (couponDate.equals(bond.getMaturityDate())) {
                cashFlow += bond.getFaceValue();
            }

            weightedPVSum += timeInYears * (cashFlow / Math.pow(1 + periodRate, exponent));
        }

        return weightedPVSum / pricer.dirtyPrice(bond, ytm);
    }
}