package com.examplecode;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
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

        LocalDate settlement = LocalDate.of(2024, 3, 20);

        for (Bond b : bonds) {
            //System.out.println(b);

            double dirty = pricer.dirtyPrice(b, settlement, 0.06);
            double accrued = pricer.accruedInterest(b, settlement);
            double clean = pricer.cleanPrice(b, settlement, 0.06);

            System.out.println("Bond: " + b);
            System.out.printf("  Dirty Price:       $%,.2f%n", dirty);
            System.out.printf("  Accrued Interest:  $%,.2f%n", accrued);
            System.out.printf("  Clean Price:       $%,.2f%n", clean);
        }
    }
}
