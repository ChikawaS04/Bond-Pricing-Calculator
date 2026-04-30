package com.examplecode;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CsvParser {
    public List<Bond> parseBonds(Path filePath) throws IOException {

        List<Bond> bondList = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String header = reader.readLine(); // skip header
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",");
                if (parts.length < 9) continue; // skip incomplete lines
                bondList.add(parseBond(parts));
            }
        }
        return bondList;
    }

    private Bond parseBond(String[] parts) {
        return new Bond(
                parts[0].trim(),
                parts[1].trim(),
                Double.parseDouble(parts[2].trim()),
                Double.parseDouble(parts[3].trim()),
                Integer.parseInt(parts[4].trim()),
                Integer.parseInt(parts[5].trim()),
                LocalDate.parse(parts[6].trim()),
                LocalDate.parse(parts[7].trim()),
                parts[8].trim()
        );
    }
}

