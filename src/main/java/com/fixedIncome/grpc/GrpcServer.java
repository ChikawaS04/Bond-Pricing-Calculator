package com.fixedIncome.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Starts the gRPC server hosting {@link PricingServiceImpl} and {@link SensitivityServiceImpl}.
 */
public class GrpcServer {

    private static final Logger logger = Logger.getLogger(GrpcServer.class.getName());
    private static final int PORT = 9090;

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(PORT)
                .addService(new PricingServiceImpl())
                .addService(new SensitivityServiceImpl())
                .build()
                .start();

        logger.info("gRPC server started on port " + PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gRPC server...");
            server.shutdown();
            logger.info("Server shut down.");
        }));

        server.awaitTermination();
    }
}
