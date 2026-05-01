package com.examplecode;

import com.examplecode.parser.CsvParser;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Entry point demonstrating bond pricing with {@link BondPricer}.
 *
 * <p>Creates a sample 5-year, 5% semi-annual bond issued 2023-01-15 and
 * prices it as of settlement date 2024-03-20 at a 6% yield, printing the
 * dirty price, accrued interest, and clean price.</p>
 */
public class BondApplication {
    static void main(String[] args) throws IOException {

        BondPricer pricer = new BondPricer();
        CsvParser bondParser = new CsvParser();
        List<Bond> bonds = bondParser.parseBonds(Path.of("BondData.csv"));


        for (Bond bond : bonds) {
            // Step 1: solve recomputed yield against the market dirty price from the CSV
            double recomputedYield = pricer.solveYTM(bond, bond.getDirtyPrice());

            // Step 2: price using recomputed yield, not the vendor-quoted yield
            double clean = pricer.cleanPrice(bond, recomputedYield);
            double dirty = pricer.dirtyPrice(bond, recomputedYield);
            double accrued = pricer.accruedInterest(bond);

            // Step 3: sanity check -- recomputed yield vs vendor-quoted yield
            double yieldDiff = Math.abs(recomputedYield - bond.getQuotedYield());

            System.out.println("Bond: " + bond);
            System.out.printf("  Clean Price:       $%,.2f%n", clean);
            System.out.printf("  Dirty Price:       $%,.2f%n", dirty);
            System.out.printf("  Accrued Interest:  $%,.2f%n", accrued);
            System.out.printf("  Recomputed Yield:  %.2f%%%n", recomputedYield * 100);
        }
    }
}
