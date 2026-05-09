package com.fixedIncome;

import com.fixedIncome.pricer.FixedRateBondPricer;
import com.fixedIncome.parser.BondCSVParser;
import com.fixedIncome.securities.Bond;
import com.fixedIncome.sensitivityanalysis.BondDV01;
import com.fixedIncome.sensitivityanalysis.BondMacaulayDuration;
import com.fixedIncome.sensitivityanalysis.BondModifiedDuration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

/**
 * Application entry point for batch bond pricing and sensitivity analysis.
 *
 * <p>Reads bond data from {@code BondData.csv}, then prices each bond concurrently
 * using a thread pool sized to the available processors. For each bond, the following
 * are computed and printed:</p>
 * <ul>
 *   <li>Recomputed yield-to-maturity (solved from the market dirty price via Newton-Raphson)</li>
 *   <li>Recomputed clean price and dirty price</li>
 *   <li>Accrued interest</li>
 *   <li>Macaulay duration in years</li>
 * </ul>
 *
 * <p>Pricing errors for individual bonds are caught and reported to stderr without
 * aborting the remaining results.</p>
 */
public class BondApplication {

    /**
     * Holds all computed outputs for a single bond so that results can be
     * collected from concurrent tasks and printed in order.
     */
    record BondResult(Bond bond, double clean, double dirty, double accrued, double yield, double macaulay, double modified, double dv01) {}

    public static void main(String[] args) throws IOException, InterruptedException {

        FixedRateBondPricer fixedRateBondPricer = new FixedRateBondPricer();
        BondCSVParser bondParser = new BondCSVParser();
        BondMacaulayDuration bondMacaulay = new BondMacaulayDuration();
        BondModifiedDuration bondModifiedDuration = new BondModifiedDuration();
        BondDV01 bondDV01 = new BondDV01();
        List<Bond> bonds = bondParser.parseBonds(Path.of("BondData.csv"));

        try (ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            List<Callable<BondResult>> tasks = bonds.stream().map(bond -> (Callable<BondResult>) () -> {
                double recomputedYield = fixedRateBondPricer.solveYTM(bond, bond.getDirtyPrice());
                double clean = fixedRateBondPricer.cleanPrice(bond, recomputedYield);
                double dirty = fixedRateBondPricer.dirtyPrice(bond, recomputedYield);
                double accrued = fixedRateBondPricer.accruedInterest(bond);
                double macaulay = bondMacaulay.calculateMacaulayDuration(bond, recomputedYield);
                double modified = bondModifiedDuration.calculateModifiedDuration(bondMacaulay, bond, recomputedYield);
                double dv01 = bondDV01.calculateDV01(bondModifiedDuration, fixedRateBondPricer, bond, recomputedYield);
                return new BondResult(bond, clean, dirty, accrued, recomputedYield, macaulay, modified, dv01);
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
                    System.out.printf("  Macaulay Duration: %.4f years%n", r.macaulay());
                    System.out.printf("  Modified Duration: %.4f years%n", r.modified());
                    System.out.printf("  DV01:              $%,.4f%n", r.dv01());
                } catch (ExecutionException e) {
                    System.err.println("Error pricing bond: " + e.getCause().getMessage());
                }
            }
        }
    }
}