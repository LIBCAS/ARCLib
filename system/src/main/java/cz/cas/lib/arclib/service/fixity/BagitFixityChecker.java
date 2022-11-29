package cz.cas.lib.arclib.service.fixity;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.bpm.IngestTool;
import cz.cas.lib.arclib.domain.HashType;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
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
    private FixityCounterFacade fixityCounterFacade;

    /**
     * Verifies fixity of every file specified in manifest files of the package.
     * Currently supports MD5, SHA-1, SHA-256 and SHA-512.
     *
     * @param pathToFixityFile redundant argument, the path must be equal to the sipWsPath for this implementation
     *                         of {@link FixityChecker}
     */
    @Override
    public void verifySIP(Path sipWsPath, Path pathToFixityFile, String externalId, JsonNode configRoot, Map<String, Pair<String, String>> formatIdentificationResult,
                          int fixityCheckToolCounter, IngestTool fixityCheckerTool)
            throws IncidentException, IOException {
        log.debug("Verifying fixity of SIP of type Bagit, package root path: " + sipWsPath);

        Map<Path, List<Path>> invalidFixitiesWrapper = new HashMap<>();
        Map<Path, List<Path>> missingFilesWrapper = new HashMap<>();
        Map<Path, Map<String, List<Path>>> unsupportedChecksumTypesWrapper = new HashMap<>();

        Pattern fileNamePattern = Pattern.compile(FILENAME_PATTERN);
        File[] files = sipWsPath.toFile().listFiles((dir, name) -> fileNamePattern.matcher(name).find());

        assert files != null;
        for (File file : files) {
            Map<String, List<Path>> unsupportedChecksumTypes = new HashMap<>();
            List<Path> invalidFixities = new ArrayList<>();
            List<Pair<Path, String>> checksumPairs = parseChecksumPairs(file.toPath(), sipWsPath);
            List<Path> pathsToFiles = checksumPairs.stream()
                    .map(Pair::getLeft)
                    .collect(Collectors.toList());

            FixityCounter counter;

            Matcher matcher = fileNamePattern.matcher(file.getName());
            matcher.find();
            String checksumType = matcher.group(1);
            switch (checksumType) {
                case "md5":
                    counter = fixityCounterFacade.getFixityCounters().get(HashType.MD5);
                    break;
                case "sha1":
                    counter = fixityCounterFacade.getFixityCounters().get(HashType.Sha1);
                    break;
                case "sha256":
                    counter = fixityCounterFacade.getFixityCounters().get(HashType.Sha256);
                    break;
                case "sha512":
                    counter = fixityCounterFacade.getFixityCounters().get(HashType.Sha512);
                    break;
                default:
                    unsupportedChecksumTypes.put(checksumType, pathsToFiles);
                    unsupportedChecksumTypesWrapper.put(file.toPath(), unsupportedChecksumTypes);
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

            List<Path> missingFiles = new ArrayList<>(pathsToMissingFiles);

            for (Pair<Path, String> checksumPair : validChecksumPairs) {
                Path filePath = checksumPair.getLeft();
                byte[] computedChecksum = counter.computeDigest(filePath);
                if (!counter.checkIfDigestsMatches(checksumPair.getRight(), computedChecksum)) {
                    invalidFixities.add(filePath);
                }
            }
            if (!invalidFixities.isEmpty())
                invalidFixitiesWrapper.put(file.toPath(), invalidFixities);
            if (!missingFiles.isEmpty())
                missingFilesWrapper.put(file.toPath(), missingFiles);
        }
        if (!unsupportedChecksumTypesWrapper.isEmpty())
            invokeUnsupportedChecksumTypeIssue(sipWsPath, unsupportedChecksumTypesWrapper, externalId, configRoot, formatIdentificationResult, fixityCheckToolCounter, fixityCheckerTool);
        if (!missingFilesWrapper.isEmpty())
            invokeMissingFilesIssue(sipWsPath, missingFilesWrapper, externalId, configRoot, fixityCheckToolCounter, fixityCheckerTool);
        if (!invalidFixitiesWrapper.isEmpty())
            invokeInvalidChecksumsIssue(sipWsPath, invalidFixitiesWrapper, externalId, configRoot, formatIdentificationResult, fixityCheckToolCounter, fixityCheckerTool);
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

    @Inject
    public void setFixityCounterFacade(FixityCounterFacade fixityCounterFacade) {
        this.fixityCounterFacade = fixityCounterFacade;
    }
}
