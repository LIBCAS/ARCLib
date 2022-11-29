package cz.cas.lib.arclib.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static cz.cas.lib.core.util.Utils.eq;

@Component
public class ExportInfoFileService {

    public static final String EXPORT_INFO_FILE_NAME = "ARCLib_export_info.csv";
    public static final String KEY_AUTHORIAL_PACKAGE_UUID = "authorial_package_uuid";

    public Map<String, String> parse(Path file) throws IOException {
        if (!file.toFile().exists()) {
            throw new IllegalArgumentException("file: " + file + " does not exist");
        }
        try (FileReader csvFileReader = new FileReader(file.toFile())) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withSkipHeaderRecord(true).parse(csvFileReader);
            Map<String, String> parsedInfoFile = new HashMap<>();
            for (CSVRecord record : records) {
                eq(record.size(), 2, () -> new IllegalStateException("some row of " + file + " contained more or less then 2 columns"));
                parsedInfoFile.put(record.get(0), record.get(1));
            }
            return parsedInfoFile;
        }
    }

    public void write(Path file, Map<String, String> metadata) throws IOException {
        try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(file.toFile()), CSVFormat.DEFAULT)) {
            for (Map.Entry<String, String> e : metadata.entrySet()) {
                csvPrinter.printRecord(e.getKey(), e.getValue());
            }
        }
    }
}
