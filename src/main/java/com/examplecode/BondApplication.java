package com.examplecode;

import com.examplecode.parser.CsvParser;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

/**
 * Entry point demonstrating bond pricing with {@link BondPricer}.
 *
 * <p>Creates a sample 5-year, 5% semi-annual bond issued 2023-01-15 and
 * prices it as of settlement date 2024-03-20 at a 6% yield, printing the
 * dirty price, accrued interest, and clean price.</p>
 */

public class BondApplication {

    record BondResult(Bond bond, double clean, double dirty, double accrued, double yield) {}

    static void main(String[] args) throws IOException, InterruptedException {

        BondPricer pricer = new BondPricer();
        CsvParser bondParser = new CsvParser();
        List<Bond> bonds = bondParser.parseBonds(Path.of("BondData.csv"));

        try (ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            List<Callable<BondResult>> tasks = bonds.stream().map(bond -> (Callable<BondResult>) () -> {
                double recomputedYield = pricer.solveYTM(bond, bond.getDirtyPrice());
                double clean = pricer.cleanPrice(bond, recomputedYield);
                double dirty = pricer.dirtyPrice(bond, recomputedYield);
                double accrued = pricer.accruedInterest(bond);
                return new BondResult(bond, clean, dirty, accrued, recomputedYield);
            }).toList();

            List<Future<BondResult>> futures = executor.invokeAll(tasks);
            for (Future<BondResult> future : futures) {
                try {
                    BondResult r = future.get();
                    System.out.println("Bond: " + r.bond());
                    System.out.printf("  Clean Price:       $%,.2f%n", r.clean());
                    System.out.printf("  Dirty Price:       $%,.2f%n", r.dirty());
                    System.out.printf("  Accrued Interest:  $%,.2f%n", r.accrued());
                    System.out.printf("  Recomputed Yield:  %.2f%%%n", r.yield() * 100);
                } catch (ExecutionException e) {
                    System.err.println("Error pricing bond: " + e.getCause().getMessage());
                }
            }
        }
    }
}
