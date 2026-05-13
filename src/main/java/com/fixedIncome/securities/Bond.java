package com.fixedIncome.securities;

import com.fixedIncome.daycount.DayCountConvention;
import com.fixedIncome.daycount.DayCountFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable representation of a fixed-rate coupon bond.
 *
 * <p>Stores the bond's static terms (face value, coupon rate, payment frequency,
 * issue/maturity dates, and day-count convention) and exposes derived values
 * such as the periodic coupon payment and the full schedule of coupon dates.</p>
 *
 * <p>All fields are validated at construction time; an {@link IllegalArgumentException}
 * is thrown for any invalid combination of inputs.</p>
 */
public final class Bond {

    private final String issuer;
    private final String ISIN;
    private final double faceValue;
    private final double couponRate;
    private final int paymentFrequency;
    private final LocalDate issueDate;
    private final LocalDate maturityDate;
    private final String dayCountConvention;
    private final String spRating;
    private final LocalDate settlementDate;
    private final double cleanPrice;
    private final double dirtyPrice;
    private final double quotedYield;

    private final double couponPayment;
    private final List<LocalDate> couponPaymentDates;
    private final DayCountConvention resolvedDayCountConvention;


    /** @return issuer of the bond, e.g., "ACME Co." */
    public String getIssuer() { return issuer; }

    /** @return ISIN of the bond, e.g., "US0378331005" */
    public String getISIN() { return ISIN; }

    /** @return face (par) value of the bond, e.g., 1000.0 */
    public double getFaceValue() {
        return faceValue;
    }

    /** @return annual coupon rate as a decimal, e.g., 0.05 for 5% */
    public double getCouponRate() {
        return couponRate;
    }

    /** @return number of coupon payments per year, e.g., 2 for semi-annual */
    public int getPaymentFrequency() {
        return paymentFrequency;
    }

    /** @return the date on which the bond was issued */
    public LocalDate getIssueDate() { return issueDate; }

    /** @return the date on which principal is repaid and the final coupon is paid */
    public LocalDate getMaturityDate() { return maturityDate; }

    /** @return the day-count convention string (always upper-case), e.g. {@code "ACT/365"} */
    public String getDayCountConvention() { return dayCountConvention; }

    public String getSpRating() { return spRating; }

    public LocalDate getSettlementDate() { return settlementDate; }

    public double getCleanPrice() { return cleanPrice; }

    public double getDirtyPrice() { return dirtyPrice; }

    public double getQuotedYield() { return quotedYield; }

    public double getCouponPayment () { return couponPayment; }

    public List<LocalDate> getCouponPaymentDates() { return couponPaymentDates; }

    public DayCountConvention getResolvedDayCountConvention() { return resolvedDayCountConvention; }

    /**
     * Constructs a Bond and validates all input parameters.
     *
     * @param issuer             issuer name (must not be null or empty)
     * @param ISIN               ISIN (must be valid)
     * @param faceValue          par value (must be &gt; 0)
     * @param couponRate         annual coupon rate as a decimal (must be &gt; 0)
     * @param paymentFrequency   coupon payments per year (must be &gt; 0)
     * @param issueDate          issue date (must not be null)
     * @param maturityDate       maturity date (must be after {@code issueDate})
     * @param dayCountConvention day-count convention name recognized by {@link DayCountFactory}
     * @throws IllegalArgumentException if any parameter fails validation
     */
    public Bond (String issuer, String ISIN, double faceValue, double couponRate, int paymentFrequency, LocalDate issueDate, LocalDate maturityDate, String dayCountConvention, String spRating, LocalDate settlementDate, double cleanPrice, double dirtyPrice, double quotedYield) {

        if (issuer == null || issuer.isEmpty()) {
            throw new IllegalArgumentException("Issuer name is required.");
        }
        if (ISIN == null || !ISIN.matches("[A-Z]{2}[A-Z0-9]{9}[0-9]")) {
            throw new IllegalArgumentException("Invalid ISIN: " + ISIN);
        }
        if (faceValue <= 0) throw new IllegalArgumentException("Face value must be > 0.");
        if (couponRate <= 0) throw new IllegalArgumentException("Coupon rate must be > 0.");

        if (issueDate == null || maturityDate == null) {
            throw new IllegalArgumentException("Issue date and maturity date are required.");
        }
        if (!maturityDate.isAfter(issueDate)) {
            throw new IllegalArgumentException("Maturity date must be after issue date.");
        }
        if (dayCountConvention == null) {
            throw new IllegalArgumentException("Day count convention is required.");
        }

        // Eagerly validate the convention string via the factory
        DayCountFactory.getConvention(dayCountConvention);

        this.issuer = issuer;
        this.ISIN = ISIN;
        this.faceValue = faceValue;
        this.couponRate = couponRate;
        this.paymentFrequency = paymentFrequency;
        this.issueDate = issueDate;
        this.maturityDate = maturityDate;
        this.dayCountConvention = dayCountConvention.toUpperCase();
        this.spRating = spRating;
        this.settlementDate = settlementDate;
        this.cleanPrice = cleanPrice;
        this.dirtyPrice = dirtyPrice;
        this.quotedYield = quotedYield;

        this.couponPayment = faceValue * couponRate / paymentFrequency;
        this.couponPaymentDates = buildCouponSchedule();
        this.resolvedDayCountConvention = DayCountFactory.getConvention(dayCountConvention, paymentFrequency);
    }

    private List<LocalDate> buildCouponSchedule() {
        List<LocalDate> dates = new ArrayList<>();
        int monthsPeriod = 12 / paymentFrequency;
        LocalDate date = issueDate.plusMonths(monthsPeriod);
        while (!date.isAfter(maturityDate)) {
            dates.add(date);
            date = date.plusMonths(monthsPeriod);
        }
        return List.copyOf(dates); // unmodifiable, no defensive copy needed downstream
    }

    @Override
    public String toString () {
        return "[Issuer= " + getIssuer() +
                ", ISIN= " + getISIN() +
                ", faceValue= " + getFaceValue() +
                ", couponRate= " + getCouponRate() +
                ", frequency= " + getPaymentFrequency() +
                ", spRating= " + getSpRating() +
                ", settlementDate= " + getSettlementDate() +
                ", cleanPrice= " + getCleanPrice() +
                ", dirtyPrice= " + getDirtyPrice() +
                ", quotedYield= " + getQuotedYield() +
                "]";
    }
}
