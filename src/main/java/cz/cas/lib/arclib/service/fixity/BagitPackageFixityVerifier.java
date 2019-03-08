package cz.cas.lib.arclib.service.fixity;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.core.util.Utils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BagitPackageFixityVerifier extends PackageFixityVerifier {

    private static final String FILENAME_PATTERN = "[tag]?manifest-(.+)\\.txt";
    private static final String FILE_LINE_PATTERN = "(\\w+)\\s+\\*?\\s*(\\S+)\\s*";
    @Getter
    private String toolName = "ARCLib_bagit_" + IngestToolFunction.fixity_check;
    @Getter
    private String toolVersion = null;

    /**
     * Verifies fixity of every file specified in manifest files of the package.
     * Currently supports MD5, SHA-1, SHA-256 and SHA-512.
     *
     * @param sipWsPath       path to SIP in workspace
     * @param packageRootPath path to the root of the package
     * @param externalId      external id of the ingest workflow
     * @param configRoot      root node of the ingest workflow JSON config containing configuration of the behaviour
     *                        for a case of a fixity error
     * @return list of associated values in triplets: file path, type of fixity, fixity value.
     */
    @Override
    public List<Utils.Triplet<String, String, String>> verifySIP(Path sipWsPath, Path packageRootPath, String externalId, JsonNode configRoot, Map<String, Utils.Pair<String, String>> formatIdentificationResult)
            throws IncidentException, IOException {
        log.debug("Verifying fixity of SIP of type Bagit, package root path: " + packageRootPath);

        List<Path> missingFiles = new ArrayList<>();
        List<Path> invalidFixities = new ArrayList<>();
        Map<String, List<Path>> unsupportedChecksumTypes = new HashMap<>();

        Pattern fileNamePattern = Pattern.compile(FILENAME_PATTERN);
        File[] files = packageRootPath.toFile().listFiles((dir, name) -> fileNamePattern.matcher(name).find());

        List<Utils.Triplet<String, String, String>> filePathsAndFixities = new ArrayList<>();

        for (File file : files) {
            List<Utils.Pair<Path, String>> checksumPairs = parseChecksumPairs(file.toPath(), packageRootPath);
            List<Path> pathsToFiles = checksumPairs.stream()
                    .map(Utils.Pair::getL)
                    .collect(Collectors.toList());

            FixityCounter counter;

            Matcher matcher = fileNamePattern.matcher(file.getName());
            matcher.find();
            String checksumType = matcher.group(1);
            switch (checksumType) {
                case "md5":
                    counter = md5Counter;
                    break;
                case "sha1":
                    counter = sha1Counter;
                    break;
                case "sha256":
                    counter = sha256Counter;
                    break;
                case "sha512":
                    counter = sha512Counter;
                    break;
                default:
                    unsupportedChecksumTypes.put(checksumType, pathsToFiles);
                    continue;
            }

            List<Utils.Pair<Path, String>> validChecksumPairs = checksumPairs.stream()
                    .filter(p -> p.getL().toFile().isFile())
                    .collect(Collectors.toList());

            List<Path> pathsToExistingFiles = validChecksumPairs.stream()
                    .map(Utils.Pair::getL)
                    .collect(Collectors.toList());

            List<Path> pathsToMissingFiles = new ArrayList<>(pathsToFiles);
            pathsToMissingFiles.removeAll(pathsToExistingFiles);

            missingFiles.addAll(pathsToMissingFiles);

            for (Utils.Pair<Path, String> checksumPair : validChecksumPairs) {
                Path filePath = checksumPair.getL();
                String checksumValue = checksumPair.getR();

                if (!counter.verifyFixity(filePath, checksumValue)) {
                    invalidFixities.add(filePath);
                }
                filePathsAndFixities.add(new Utils.Triplet(filePath.toString(), checksumType, checksumValue));
            }
        }
        if (!unsupportedChecksumTypes.isEmpty())
            invokeUnsupportedChecksumTypeIssue(sipWsPath, unsupportedChecksumTypes, externalId, configRoot, formatIdentificationResult);
        if (!missingFiles.isEmpty())
            invokeMissingFilesIssue(sipWsPath, missingFiles, externalId, configRoot, formatIdentificationResult);
        if (!invalidFixities.isEmpty())
            invokeInvalidChecksumsIssue(sipWsPath, invalidFixities, externalId, configRoot, formatIdentificationResult);

        return filePathsAndFixities;
    }

    /**
     * Parses checksum pairs
     *
     * @param manifestFile file containing the fixities
     * @param packageRoot  path to the root of the package
     * @return list of pairs of file paths to checksum values
     * @throws IOException <code>manifestFile</code> could not be read
     */
    private List<Utils.Pair<Path, String>> parseChecksumPairs(Path manifestFile, Path packageRoot) throws IOException {
        Pattern fileLinePattern = Pattern.compile(FILE_LINE_PATTERN);
        List<Utils.Pair<Path, String>> checksumPairs = new ArrayList<>();
        for (String line : Files.readAllLines(manifestFile)) {
            Matcher matcher = fileLinePattern.matcher(line);
            if (!matcher.find()) {
                log.warn("Unable to parse manifest line: " + line);
                continue;
            }
            checksumPairs.add(new Utils.Pair<>(packageRoot.resolve(matcher.group(2)).normalize().toAbsolutePath(), matcher.group(1)));
        }
        return checksumPairs;
    }
}
