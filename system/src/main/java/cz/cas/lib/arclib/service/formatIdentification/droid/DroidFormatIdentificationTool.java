package cz.cas.lib.arclib.service.formatIdentification.droid;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.service.formatIdentification.FormatIdentificationTool;
import cz.cas.lib.arclib.service.formatIdentification.FormatIdentificationToolType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static cz.cas.lib.core.util.Utils.*;

@Slf4j
public class DroidFormatIdentificationTool extends FormatIdentificationTool {

    private static final String FILTER = "type any FILE CONTAINER";
    private static final String CMD;
    public static final String FORMAT_IDENTIFIER_NAME = FormatIdentificationToolType.DROID.toString();
    static {
        if (SystemUtils.IS_OS_WINDOWS) {
            CMD = "droid.bat";
        } else {
            CMD = "droid";
        }
    }

    /**
     * Column to parse from the .CSV with the identification result
     */
    @Getter
    private CsvResultColumn parsedColumn;

    public DroidFormatIdentificationTool(CsvResultColumn parsedColumn) {
        this.parsedColumn = parsedColumn;
    }

    public Map<String, List<Pair<String, String>>> analyze(Path pathToSip) throws IOException {
        notNull(pathToSip, () -> {
            throw new IllegalArgumentException("null path to SIP package");
        });
        if (!pathToSip.toFile().exists()) {
            throw new FileNotFoundException("no file/folder found at: " + pathToSip);
        }

        log.debug("DROID format analysis for SIP at path " + pathToSip + " started.");

        Path profileResultsPath = Paths.get(pathToSip + ".droid");
        Path exportResultsPath = Paths.get(pathToSip + ".csv");
        try {
            runProfile(pathToSip, profileResultsPath);
            exportProfile(profileResultsPath, exportResultsPath);

            log.debug("DROID format analysis for SIP at path " + pathToSip + " finished.");
            return parseResults(exportResultsPath, parsedColumn, pathToSip);
        } finally {
            cleanUp(asList(profileResultsPath, exportResultsPath));
        }
    }

    /**
     * Runs DROID that creates and runs a new profile using the files belonging the SIP
     *
     * @param pathToSIP    path to the SIP to analyze
     * @param pathToResult path to the <i>.DROID</i> file with the result of the profile
     */
    protected void runProfile(Path pathToSIP, Path pathToResult) {
        executeProcessDefaultResultHandle(CMD, "-R", "-a", pathToSIP.toAbsolutePath().toString(), "-p",
                pathToResult.toAbsolutePath().toString());
        log.debug("File with DROID profile result created at " + pathToResult + ".");
    }

    /**
     * Runs DROID that exports the results of the specified profile to a CSV file with one row for each format for each file profiled
     * (if a file has multiple identifications, then a separate row will be written out for each file and separate identification made)
     *
     * @param pathToProfile path to the <i>.DROID</i> file with the result of a profile
     * @param pathToResult  path to the <i>CSV</i> file with the result of the export of profile
     */
    protected void exportProfile(Path pathToProfile, Path pathToResult) throws FileNotFoundException {
        if (!pathToProfile.toFile().exists()) {
            throw new FileNotFoundException("File with the profile of the DROID format identification" +
                    " does not exist at the path : " + pathToProfile);
        }
        executeProcessDefaultResultHandle(CMD, "-p", pathToProfile.toAbsolutePath().toString(), "-f", FILTER, "-E",
                pathToResult.toAbsolutePath().toString());
        log.debug("File with DROID export result created at " + pathToResult + ".");
    }

    /**
     * From the CSV file with the exported profile parses the values of the specified column
     *
     * @param pathToResultsCsv path to the CSV file to parse
     * @param parsedColumn     column of which respective values will appear in the result as values
     * @param pathToSip        path to the SIP package
     * @return map of key-value pairs where the key is a path to a file and the value is a list of pairs of a value
     * and corresponding identification method (multiple values can correspond to a single file path)
     * @throws GeneralException CSV file with the results of the DROID format identification is inaccessible
     */
    protected Map<String, List<Pair<String, String>>> parseResults(Path pathToResultsCsv, CsvResultColumn parsedColumn, Path pathToSip) throws IOException {
        log.debug("Parsing of CSV file " + pathToResultsCsv + " started.");

        Map<String, List<Pair<String, String>>> filePathsToParsedColumnValues = new HashMap<>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(pathToResultsCsv.toAbsolutePath().toString()));

            Iterable<CSVRecord> records = CSVFormat.EXCEL.withDelimiter(',').withHeader(
                    "ID", "PARENT_ID", "URI", "FILE_PATH", "NAME", "METHOD", "STATUS", "SIZE", "TYPE", "EXT",
                    "LAST_MODIFIED", "EXTENSION_MISMATCH", "HASH", "FORMAT_COUNT", "PUID", "MIME_TYPE", "FORMAT_NAME",
                    "FORMAT_VERSION"
            ).withSkipHeaderRecord(true).parse(br);

            String pathToSipStr = pathToSip.toAbsolutePath().toString().replace("\\", "/");

            for (CSVRecord record : records) {
                String filePath = record.get(CsvResultColumn.URI);
                String parsedColumnValue = record.get(parsedColumn.name());
                String method = record.get(CsvResultColumn.METHOD.name());

                filePath = filePath.replaceAll("file:/?" + Pattern.quote(pathToSipStr) + "/", "");

                List<Pair<String, String>> parsedColumnValues = filePathsToParsedColumnValues.get(filePath);
                if (parsedColumnValues == null) {
                    parsedColumnValues = new ArrayList<>();
                }
                parsedColumnValues.add(Pair.of(parsedColumnValue, method));
                filePathsToParsedColumnValues.put(filePath, parsedColumnValues);

                log.debug("File at path \"" + filePath + "\" has been identified with format: " + parsedColumnValue +
                        ". Identification method: " + method + ".");
            }
        } catch (IOException e) {
            throw new GeneralException("CSV file with the results of the DROID format identification" +
                    " is inaccessible at the path: " + pathToResultsCsv, e);
        } finally {
            if (br != null) {
                br.close();
            }
        }

        log.debug("Parsing of CSV file " + pathToResultsCsv + " finished.");
        return filePathsToParsedColumnValues;
    }

    public String getToolName() {
        return FORMAT_IDENTIFIER_NAME;
    }

    /**
     * Returns a string containing the version of format identifier together with the versions of the signature files.
     *
     * @return string with the format identifier version
     */
    public String getToolVersion() {
        String toolVersion = "DROID: version: " + getDroidVersion().get(0) + ", Signature files: ";
        List<String> droidSignatureFilesVersions = getDroidSignatureFilesVersions();
        //sort alphabetically
        java.util.Collections.sort(droidSignatureFilesVersions);

        for (int i = 0; i < droidSignatureFilesVersions.size(); i++) {
            String sigFileVers = droidSignatureFilesVersions.get(i);
            log.debug(sigFileVers + " ");
            toolVersion += i + 1 + ". " + sigFileVers + " ";
        }
        return toolVersion;
    }

    /**
     * Get the names of DROID's current default signature file and container signature
     *
     * @return names of signature file and container signature
     */
    protected List<String> getDroidSignatureFilesVersions() {
        Pair<Integer, List<String>> result = executeProcessCustomResultHandle(false, CMD, "-x");
        if (result.getLeft() != 0)
            throw new IllegalStateException("Droid signature files version CMD has failed: " + result.getRight());
        return result.getRight();
    }

    /**
     * Get the version of DROID
     *
     * @return name of the current DROID version
     */
    protected List<String> getDroidVersion() {
        Pair<Integer, List<String>> result = executeProcessCustomResultHandle(false, CMD, "-v");
        if (result.getLeft() != 0)
            throw new IllegalStateException("Droid version CMD has failed: " + result.getRight());
        return result.getRight();
    }

    /**
     * Deletes specified files created by DROID
     *
     * @param filesToDelete list of file paths to the files to delete
     */
    private void cleanUp(List<Path> filesToDelete) {
        filesToDelete.forEach(f -> {
            try {
                Files.deleteIfExists(f);
            } catch (IOException e) {
            }
        });
    }
}
