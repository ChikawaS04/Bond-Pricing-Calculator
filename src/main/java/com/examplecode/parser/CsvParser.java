package com.examplecode.parser;

import com.examplecode.Bond;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses bond data from a CSV file into a list of {@link Bond} objects.
 *
 * <p>Expected CSV format (comma-separated, with header row):
 * <pre>
 * issuer,ISIN,faceValue,couponRate,yearsToMaturity,paymentFrequency,issueDate,maturityDate,dayCountConvention
 * </pre>
 * Dates must be in ISO-8601 format (yyyy-MM-dd). Blank lines and rows with
 * fewer than 9 fields are silently skipped.</p>
 */
public class CsvParser {

    /**
     * Reads the CSV file at {@code filePath} and returns all valid bonds parsed from it.
     *
     * @param filePath path to the CSV file
     * @return list of parsed {@link Bond} objects; never null, may be empty
     * @throws IOException if the file cannot be opened or read
     */
    public List<Bond> parseBonds(Path filePath) throws IOException {

        List<Bond> bondList = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String header = reader.readLine(); // skip header row
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;           // ignore empty lines
                String[] parts = line.split(",");
                if (parts.length < 9) continue;         // ignore incomplete rows
                bondList.add(parseBond(parts));
            }
        }
        return bondList;
    }

    /**
     * Constructs a {@link Bond} from a pre-split array of CSV fields.
     *
     * <p>Field order must match the expected CSV column layout:
     * <ol start="0">
     *   <li>issuer name</li>
     *   <li>ISIN</li>
     *   <li>face value (double)</li>
     *   <li>coupon rate as a decimal, e.g. {@code 0.05} for 5% (double)</li>
     *   <li>years to maturity (int)</li>
     *   <li>payment frequency — coupons per year (int)</li>
     *   <li>issue date (yyyy-MM-dd)</li>
     *   <li>maturity date (yyyy-MM-dd)</li>
     *   <li>day-count convention, e.g. {@code ACT/365}</li>
     * </ol>
     *
     * @param parts array of raw field strings from one CSV row
     * @return a fully constructed {@link Bond}
     */
    private Bond parseBond(String[] parts) {
        return new Bond(
                parts[0].trim(),                            // issuer
                parts[1].trim(),                            // ISIN
                Double.parseDouble(parts[2].trim()),        // face value
                Double.parseDouble(parts[3].trim()),        // coupon rate
                Integer.parseInt(parts[4].trim()),          // payment frequency
                LocalDate.parse(parts[5].trim()),           // issue date
                LocalDate.parse(parts[6].trim()),           // maturity date
                parts[7].trim(),                             // day-count convention
                parts[8].trim(),                            // sp rating
                LocalDate.parse(parts[9].trim()),           // settlement date
                Double.parseDouble(parts[10].trim()),       // clean price
                Double.parseDouble(parts[11].trim()),       // dirty price
                Double.parseDouble(parts[12].trim())        // quoted yield
        );
    }
}

