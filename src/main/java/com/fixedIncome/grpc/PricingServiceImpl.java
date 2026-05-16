package com.fixedIncome.grpc;

import com.fixedIncome.pricer.FixedRateBondPricer;
import com.fixedIncome.securities.Bond;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * gRPC service implementation for bond pricing operations.
 * Delegates to {@link FixedRateBondPricer} after converting the incoming
 * {@link BondMessage} to a domain {@link Bond} via {@link BondMapper}.
 */
public class PricingServiceImpl extends PricingServiceGrpc.PricingServiceImplBase {

    private final FixedRateBondPricer pricer = new FixedRateBondPricer();

    /**
     * Returns the dirty price, clean price, and accrued interest for a bond at a given YTM.
     *
     * @param request          contains a {@link BondMessage} and a pre-solved YTM
     * @param responseObserver receives a single {@link PriceResponse} or an error status
     */
    @Override
    public void getPrice(PriceRequest request, StreamObserver<PriceResponse> responseObserver) {
        try {
            Bond bond = BondMapper.toDomain(request.getBond());
            double ytm = request.getYtm();

            double dirty  = pricer.dirtyPrice(bond, ytm);
            double accrued = pricer.accruedInterest(bond);
            double clean  = dirty - accrued;

            responseObserver.onNext(PriceResponse.newBuilder()
                    .setDirtyPrice(dirty)
                    .setCleanPrice(clean)
                    .setAccruedInterest(accrued)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    /**
     * Solves for the yield-to-maturity that reproduces the given target dirty price.
     *
     * @param request          contains a {@link BondMessage} and the target dirty price
     * @param responseObserver receives a single {@link SolveYTMResponse} with the solved YTM,
     *                         or {@code OUT_OF_RANGE} if the solver diverges
     */
    @Override
    public void solveYTM(SolveYTMRequest request, StreamObserver<SolveYTMResponse> responseObserver) {
        try {
            Bond bond = BondMapper.toDomain(request.getBond());
            double ytm = pricer.solveYTM(bond, request.getTargetDirtyPrice());

            responseObserver.onNext(SolveYTMResponse.newBuilder()
                    .setYtm(ytm)
                    .build());
            responseObserver.onCompleted();
        } catch (ArithmeticException e) {
            responseObserver.onError(Status.OUT_OF_RANGE
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    /**
     * Server-streaming variant of {@link #getPrice}: streams one {@link PriceResponse}
     * per entry in the batch request.
     *
     * @param request          contains a list of {@link PriceRequest} entries
     * @param responseObserver receives one response per bond, then {@code onCompleted}
     */
    @Override
    public void getPriceBatch(BatchPriceRequest request, StreamObserver<PriceResponse> responseObserver) {
        try {
            for (PriceRequest req : request.getRequestsList()) {
                Bond bond   = BondMapper.toDomain(req.getBond());
                double ytm  = req.getYtm();
                double dirty   = pricer.dirtyPrice(bond, ytm);
                double accrued = pricer.accruedInterest(bond);

                responseObserver.onNext(PriceResponse.newBuilder()
                        .setDirtyPrice(dirty)
                        .setCleanPrice(dirty - accrued)
                        .setAccruedInterest(accrued)
                        .build());
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    /**
     * Server-streaming variant of {@link #solveYTM}: streams one {@link SolveYTMResponse}
     * per entry in the batch request.
     *
     * @param request          contains a list of {@link SolveYTMRequest} entries
     * @param responseObserver receives one solved YTM per bond, then {@code onCompleted}
     */
    @Override
    public void solveYTMBatch(BatchSolveYTMRequest request, StreamObserver<SolveYTMResponse> responseObserver) {
        try {
            for (SolveYTMRequest req : request.getRequestsList()) {
                Bond bond = BondMapper.toDomain(req.getBond());
                double ytm = pricer.solveYTM(bond, req.getTargetDirtyPrice());

                responseObserver.onNext(SolveYTMResponse.newBuilder()
                        .setYtm(ytm)
                        .build());
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
