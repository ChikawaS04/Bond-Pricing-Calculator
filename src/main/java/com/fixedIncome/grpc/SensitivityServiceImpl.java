package com.fixedIncome.grpc;

import com.fixedIncome.pricer.FixedRateBondPricer;
import com.fixedIncome.securities.Bond;
import com.fixedIncome.sensitivityanalysis.BondDV01;
import com.fixedIncome.sensitivityanalysis.BondMacaulayDuration;
import com.fixedIncome.sensitivityanalysis.BondModifiedDuration;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * gRPC service implementation for bond sensitivity analysis.
 * Returns Macaulay duration, modified duration, and DV01 in a single response.
 */
public class SensitivityServiceImpl extends SensitivityServiceGrpc.SensitivityServiceImplBase {

    private final FixedRateBondPricer pricer             = new FixedRateBondPricer();
    private final BondMacaulayDuration macaulayDuration  = new BondMacaulayDuration();
    private final BondModifiedDuration modifiedDuration  = new BondModifiedDuration();
    private final BondDV01 dv01                          = new BondDV01();

    @Override
    public void getSensitivity(SensitivityRequest request, StreamObserver<SensitivityResponse> responseObserver) {
        try {
            Bond bond  = BondMapper.toDomain(request.getBond());
            double ytm = request.getYtm();

            double mac = macaulayDuration.calculateMacaulayDuration(bond, ytm);
            double mod = modifiedDuration.calculateModifiedDuration(macaulayDuration, bond, ytm);
            double dv  = dv01.calculateDV01(modifiedDuration, pricer, bond, ytm);

            responseObserver.onNext(SensitivityResponse.newBuilder()
                    .setMacauleyDuration(mac)
                    .setModifiedDuration(mod)
                    .setDv01(dv)
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
    public void getSensitivityBatch(BatchSensitivityRequest request, StreamObserver<SensitivityResponse> responseObserver) {
        try {
            for (SensitivityRequest req : request.getRequestsList()) {
                Bond bond  = BondMapper.toDomain(req.getBond());
                double ytm = req.getYtm();

                double mac = macaulayDuration.calculateMacaulayDuration(bond, ytm);
                double mod = modifiedDuration.calculateModifiedDuration(macaulayDuration, bond, ytm);
                double dv  = dv01.calculateDV01(modifiedDuration, pricer, bond, ytm);

                responseObserver.onNext(SensitivityResponse.newBuilder()
                        .setMacauleyDuration(mac)
                        .setModifiedDuration(mod)
                        .setDv01(dv)
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
