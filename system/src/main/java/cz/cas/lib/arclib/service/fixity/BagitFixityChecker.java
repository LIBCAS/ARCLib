package cz.cas.lib.arclib.service.fixity;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
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
public class BagitFixityChecker extends FixityChecker {

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
     * @param pathToFixityFile redundant argument, the path must be equal to the sipWsPath for this implementation
     *                         of {@link FixityChecker}
     */
    @Override
    public void verifySIP(Path sipWsPath, Path pathToFixityFile, String externalId, JsonNode configRoot, Map<String, Pair<String, String>> formatIdentificationResult)
            throws IncidentException, IOException {
        log.debug("Verifying fixity of SIP of type Bagit, package root path: " + pathToFixityFile);

        List<Path> missingFiles = new ArrayList<>();
        List<Path> invalidFixities = new ArrayList<>();
        Map<String, List<Path>> unsupportedChecksumTypes = new HashMap<>();

        Pattern fileNamePattern = Pattern.compile(FILENAME_PATTERN);
        File[] files = pathToFixityFile.toFile().listFiles((dir, name) -> fileNamePattern.matcher(name).find());

        for (File file : files) {
            List<Pair<Path, String>> checksumPairs = parseChecksumPairs(file.toPath(), pathToFixityFile);
            List<Path> pathsToFiles = checksumPairs.stream()
                    .map(Pair::getLeft)
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

            List<Pair<Path, String>> validChecksumPairs = checksumPairs.stream()
                    .filter(p -> p.getLeft().toFile().isFile())
                    .collect(Collectors.toList());

            List<Path> pathsToExistingFiles = validChecksumPairs.stream()
                    .map(Pair::getLeft)
                    .collect(Collectors.toList());

            List<Path> pathsToMissingFiles = new ArrayList<>(pathsToFiles);
            pathsToMissingFiles.removeAll(pathsToExistingFiles);

            missingFiles.addAll(pathsToMissingFiles);

            for (Pair<Path, String> checksumPair : validChecksumPairs) {
                Path filePath = checksumPair.getLeft();
                byte[] computedChecksum = counter.computeDigest(filePath);
                if (!counter.checkIfDigestsMatches(checksumPair.getRight(), computedChecksum)) {
                    invalidFixities.add(filePath);
                }
            }
        }
        if (!unsupportedChecksumTypes.isEmpty())
            invokeUnsupportedChecksumTypeIssue(sipWsPath, unsupportedChecksumTypes, externalId, configRoot, formatIdentificationResult);
        if (!missingFiles.isEmpty())
            invokeMissingFilesIssue(sipWsPath, missingFiles, externalId, configRoot, formatIdentificationResult);
        if (!invalidFixities.isEmpty())
            invokeInvalidChecksumsIssue(sipWsPath, invalidFixities, externalId, configRoot, formatIdentificationResult);
    }

    /**
     * Parses checksum pairs
     *
     * @param manifestFile file containing the fixities
     * @param packageRoot  path to the root of the package
     * @return list of pairs of file paths to checksum values
     * @throws IOException <code>manifestFile</code> could not be read
     */
    private List<Pair<Path, String>> parseChecksumPairs(Path manifestFile, Path packageRoot) throws IOException {
        Pattern fileLinePattern = Pattern.compile(FILE_LINE_PATTERN);
        List<Pair<Path, String>> checksumPairs = new ArrayList<>();
        for (String line : Files.readAllLines(manifestFile)) {
            Matcher matcher = fileLinePattern.matcher(line);
            if (!matcher.find()) {
                log.warn("Unable to parse manifest line: " + line);
                continue;
            }
            checksumPairs.add(Pair.of(packageRoot.resolve(matcher.group(2)).normalize().toAbsolutePath(), matcher.group(1)))
            ;
        }
        return checksumPairs;
    }
}