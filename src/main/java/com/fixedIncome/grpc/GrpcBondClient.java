package com.fixedIncome.grpc;

import com.fixedIncome.parser.BondCSVParser;
import com.fixedIncome.securities.Bond;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * gRPC client that replicates the BondApplication workflow over the network:
 * for each bond, it calls SolveYTM → GetPrice → GetSensitivity and prints results.
 */
public class GrpcBondClient {

    private static final Logger logger = Logger.getLogger(GrpcBondClient.class.getName());

    private final PricingServiceGrpc.PricingServiceBlockingStub pricingStub;
    private final SensitivityServiceGrpc.SensitivityServiceBlockingStub sensitivityStub;

    public GrpcBondClient(ManagedChannel channel) {
        pricingStub     = PricingServiceGrpc.newBlockingStub(channel);
        sensitivityStub = SensitivityServiceGrpc.newBlockingStub(channel);
    }

    public void priceBonds(List<Bond> bonds) {
        for (Bond bond : bonds) {
            BondMessage bondMsg = BondMapper.toProto(bond);
            try {
                // Step 1: solve YTM from market dirty price
                SolveYTMResponse ytmResp = pricingStub.solveYTM(SolveYTMRequest.newBuilder()
                        .setBond(bondMsg)
                        .setTargetDirtyPrice(bond.getDirtyPrice())
                        .build());
                double ytm = ytmResp.getYtm();

                // Step 2: get prices at the solved YTM
                PriceResponse priceResp = pricingStub.getPrice(PriceRequest.newBuilder()
                        .setBond(bondMsg)
                        .setYtm(ytm)
                        .build());

                // Step 3: get sensitivity metrics
                SensitivityResponse sensResp = sensitivityStub.getSensitivity(SensitivityRequest.newBuilder()
                        .setBond(bondMsg)
                        .setYtm(ytm)
                        .build());

                System.out.println("Bond: " + bond);
                System.out.printf("  Clean Price:       $%,.2f%n", priceResp.getCleanPrice());
                System.out.printf("  Dirty Price:       $%,.2f%n", priceResp.getDirtyPrice());
                System.out.printf("  Accrued Interest:  $%,.2f%n", priceResp.getAccruedInterest());
                System.out.printf("  Recomputed Yield:  %.2f%%%n", ytm * 100);
                System.out.printf("  Macaulay Duration: %.4f years%n", sensResp.getMacauleyDuration());
                System.out.printf("  Modified Duration: %.4f years%n", sensResp.getModifiedDuration());
                System.out.printf("  DV01:              $%,.4f%n", sensResp.getDv01());

            } catch (StatusRuntimeException e) {
                System.err.println("RPC failed for bond " + bond.getISIN() + ": "
                        + e.getStatus().getCode() + " - " + e.getStatus().getDescription());
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        try {
            GrpcBondClient client = new GrpcBondClient(channel);
            List<Bond> bonds = new BondCSVParser().parseBonds(Path.of("BondData.csv"));
            client.priceBonds(bonds);
        } finally {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
