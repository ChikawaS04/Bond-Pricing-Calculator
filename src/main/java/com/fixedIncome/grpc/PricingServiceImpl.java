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
