package cz.cas.lib.arclib.formatidentifier.droid;

import cz.cas.lib.arclib.formatidentifier.FormatIdentifier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.SystemUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DroidFormatIdentifier implements FormatIdentifier {

    private static final String CMD;

    static {
        if (SystemUtils.IS_OS_WINDOWS) {
            CMD = "droid.bat";
        } else {
            CMD = "droid.sh";
        }
    }

    @Getter
    private String workspace;

    @Override
    public Map<String, List<String>> analyze(String sipId) throws IOException, InterruptedException {
        log.info("DROID format analysis for SIP " + sipId + " started.");

        Path pathToSip = Paths.get(workspace).resolve(sipId);

        if (!Files.exists(pathToSip)) {
            log.error("SIP at path " + pathToSip + " doest not exist.");
            throw new FileNotFoundException("no file/folder found at: " + pathToSip);
        }

        Path profileResultsPath = Paths.get(workspace).resolve(sipId + ".droid");

        getDroidSignatureFilesVersions().forEach(sigFileVers ->
                log.info("Signature file: " + sigFileVers));

        runProfile(pathToSip, profileResultsPath);

        Path exportResultsPath = Paths.get(workspace).resolve(sipId + ".csv");
        exportProfile(profileResultsPath, exportResultsPath);

        Map<String, List<String>> filePathsToParsedColumnValues = parseResults(exportResultsPath, CsvResultColumn.PUID, pathToSip);

        log.info("DROID format analysis for SIP " + sipId + " finished.");

        return filePathsToParsedColumnValues;
    }

    /**
     * Get the names of DROID's current default signature file and container signature
     *
     * @return names of signature file and container signature
     * @throws InterruptedException
     * @throws IOException
     */
    protected List<String> getDroidSignatureFilesVersions() throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder(CMD, "-x");
        Process p = pb.start();
        p.waitFor();

        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

        List<String> signatureFileNames = new ArrayList<>();

        String line = br.readLine();
        while (line != null) {
            signatureFileNames.add(line);
            line = br.readLine();
        }

        return signatureFileNames;
    }

    /**
     * Runs DROID that creates and runs a new profile from the files belonging the SIP
     *
     * @param pathToSIP    path to the SIP to analyze
     * @param pathToResult path to the <i>.DROID</i> file with the result of the profile
     * @throws IOException
     * @throws InterruptedException
     */
    protected void runProfile(Path pathToSIP, Path pathToResult) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(CMD, "-R", "-a", pathToSIP.toAbsolutePath().toString(), "-p",
                pathToResult.toAbsolutePath().toString());
        Process p = pb.start();
        p.waitFor();

        log.info("File with DROID profile result created at " + pathToResult + ".");
    }

    /**
     * Runs DROID that exports the results of the specified profile to a CSV file with one row for each format for each file profiled
     * (if a file has multiple identifications, then a separate row will be written out for each file and separate identification made)
     *
     * @param pathToProfile path to the <i>.DROID</i> file with the result of a profile
     * @param pathToResult  path to the <i>CSV</i> file with the result of the export of profile
     * @throws IOException
     * @throws InterruptedException
     */
    protected void exportProfile(Path pathToProfile, Path pathToResult) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(CMD, "-p", pathToProfile.toAbsolutePath().toString(), "-E",
                pathToResult.toAbsolutePath().toString());
        Process p = pb.start();
        p.waitFor();

        log.info("File with DROID export result created at " + pathToResult + ".");
    }

    /**
     * From the CSV file with the exported profile parses the values of the specified column
     *
     * @param pathToResultsCsv path to the CSV file to parse
     * @param parsedColumn column of which respective values will appear in the result as values
     * @param pathToSip path to the SIP package
     * @return map of key-value pairs where the key is the path to a file and value is the list of values that appear
     * in the same row in the parsed column
     * @throws IOException
     */
    protected Map<String, List<String>> parseResults(Path pathToResultsCsv, CsvResultColumn parsedColumn, Path pathToSip) throws
            IOException {
        log.info("Parsing of CSV file " + pathToResultsCsv + " started.");

        Map<String, List<String>> filePathsToParsedColumnValues = new HashMap<>();

        final String cvsSplitBy = ",";

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(pathToResultsCsv.toAbsolutePath().toString()));
            String line = br.readLine();
            String[] header = line.split(cvsSplitBy);

            int filePathColumnIndex = -1;
            int parsedColumnIndex = -1;
            int methodColumnIndex = -1;

            //from the first line of the CSV get the index of the the parsed column and the index of the column with file path
            for (int i = 0; (i < header.length); i++) {
                //remove double quotes
                header[i] = header[i].replace("\"", "");

                if (header[i].equals(CsvResultColumn.URI.name())) {
                    filePathColumnIndex = i;
                }
                if (header[i].equals(parsedColumn.name())) {
                    parsedColumnIndex = i;
                }
                if (header[i].equals(CsvResultColumn.METHOD.name())) {
                    methodColumnIndex = i;
                }
            }

            String[] items;
            //from the following lines parse the respective column values
            while ((line = br.readLine()) != null) {
                items = line.split(cvsSplitBy);

                String parsedColumnValue = items[parsedColumnIndex].replace("\"", "");

                if (parsedColumnValue.startsWith(" ")) {
                    parsedColumnValue = items[parsedColumnIndex + 1].replace("\"", "");
                }

                String filePath = items[filePathColumnIndex];
                //remove double quotes
                filePath = filePath.replace("\"", "");
                //trim the path to SIP package to get a relative file path instead of the absolute file path
                String pathToSipStr = pathToSip.toAbsolutePath().toString().replace("\\", "/");
                filePath = filePath.replaceAll(pathToSipStr, "");

                //remove double quotes
                String methodColumnValue = items[methodColumnIndex].replace("\"", "");

                List<String> values = filePathsToParsedColumnValues.get(filePath);
                if (values == null) {
                    values = new ArrayList();
                }
                values.add(parsedColumnValue);

                log.info("File at path \"" + filePath + "\" has been identified with format: " + parsedColumnValue +
                        ". Identification method: " + methodColumnValue + ".");
                filePathsToParsedColumnValues.put(filePath, values);
            }

            filePathsToParsedColumnValues.entrySet().forEach(entry -> {
                if (entry.getValue().size() > 1) {
                    log.error("File at path \"" + entry.getKey() + "\" has been identified with multiple formats: " + entry.getValue() +
                            ", because DROID could not make a deterministic decision.");
                }
            });

            log.info("Parsing of CSV file " + pathToResultsCsv + " finished.");
        } finally {
            if (br != null) {
                br.close();
            }
        }

        return filePathsToParsedColumnValues;
    }

    @Inject
    public void setWorkspace(@Value("${arclib.workspace}") String workspace) {
        this.workspace = workspace;
    }
}
