package com.fixedIncome.grpc;

import com.fixedIncome.securities.Bond;

import java.time.LocalDate;

/**
 * Converts between the gRPC {@link BondMessage} wire type and the domain {@link Bond} object.
 */
public final class BondMapper {

    private BondMapper() {}

    /**
     * Converts a domain {@link Bond} into a {@link BondMessage} for sending over gRPC.
     */
    public static BondMessage toProto(Bond bond) {
        return BondMessage.newBuilder()
                .setIssuer(bond.getIssuer())
                .setIsin(bond.getISIN())
                .setFaceValue(bond.getFaceValue())
                .setCouponRate(bond.getCouponRate())
                .setPaymentFrequency(bond.getPaymentFrequency())
                .setIssueDate(bond.getIssueDate().toString())
                .setMaturityDate(bond.getMaturityDate().toString())
                .setDayCountConvention(bond.getDayCountConvention())
                .setSpRating(bond.getSpRating())
                .setSettlementDate(bond.getSettlementDate().toString())
                .setCleanPrice(bond.getCleanPrice())
                .setDirtyPrice(bond.getDirtyPrice())
                .setQuotedYield(bond.getQuotedYield())
                .build();
    }

    /**
     * Converts a {@link BondMessage} received over gRPC into a domain {@link Bond}.
     * Dates are expected in ISO-8601 format ("YYYY-MM-DD").
     */
    public static Bond toDomain(BondMessage msg) {
        return new Bond(
                msg.getIssuer(),
                msg.getIsin(),
                msg.getFaceValue(),
                msg.getCouponRate(),
                msg.getPaymentFrequency(),
                LocalDate.parse(msg.getIssueDate()),
                LocalDate.parse(msg.getMaturityDate()),
                msg.getDayCountConvention(),
                msg.getSpRating(),
                LocalDate.parse(msg.getSettlementDate()),
                msg.getCleanPrice(),
                msg.getDirtyPrice(),
                msg.getQuotedYield()
        );
    }
}
