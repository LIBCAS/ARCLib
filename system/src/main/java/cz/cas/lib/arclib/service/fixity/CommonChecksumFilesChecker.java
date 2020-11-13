package cz.cas.lib.arclib.service.fixity;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommonChecksumFilesChecker extends FixityChecker {

    private static final String FILE_LINE_PATTERN = "(\\w+).{2}(\\S+)";
    private static final Set<String> supportedChecksumTypes = Set.of("md5", "sha1", "sha256", "sha512");
    @Getter
    private String toolName = "ARCLib_" + IngestToolFunction.fixity_check;
    @Getter
    private String toolVersion = null;

    /**
     * Verifies fixities of all files listed in any checksum file located in SIP.
     *
     * @param pathToFixityFile redundant argument, the whole SIP is scanned for all common checksum files ({@link #supportedChecksumTypes})
     */
    @Override
    public void verifySIP(Path sipWsPath, Path pathToFixityFile, String externalId, JsonNode configRoot, Map<String, Pair<String, String>> formatIdentificationResult)
            throws IncidentException, IOException {
        List<Path> missingFiles = new ArrayList<>();
        List<Path> invalidFixities = new ArrayList<>();
        List<Path> filesWithChecksums = Files.walk(sipWsPath).filter(p -> supportedChecksumTypes.contains((FilenameUtils.getExtension(p.toFile().getName())))).collect(Collectors.toList());
        if (filesWithChecksums.isEmpty())
            return;
        log.debug("Found common checksum files, supported extensions: {}, found files: {}. Starting verification", Arrays.toString(supportedChecksumTypes.toArray()), Arrays.toString(filesWithChecksums.toArray()));
        for (Path checksumFile : filesWithChecksums) {
            log.debug("Verifying fixity of files specified in checksum file: {}", checksumFile);
            List<Pair<Path, String>> checksumPairs = parseChecksumPairs(checksumFile);
            List<Path> pathsToFiles = checksumPairs.stream()
                    .map(Pair::getLeft)
                    .collect(Collectors.toList());
            FixityCounter counter;
            switch (FilenameUtils.getExtension(checksumFile.toFile().getName())) {
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
        if (!missingFiles.isEmpty())
            invokeMissingFilesIssue(sipWsPath, missingFiles, externalId, configRoot, formatIdentificationResult);
        if (!invalidFixities.isEmpty())
            invokeInvalidChecksumsIssue(sipWsPath, invalidFixities, externalId, configRoot, formatIdentificationResult);
        log.debug("Verification of common checksum files has ended.");
    }

    /**
     * Parses checksum pairs
     *
     * @param manifestFile file containing the fixities
     * @return list of pairs of file paths to checksum values
     * @throws IOException <code>manifestFile</code> could not be read
     */
    private List<Pair<Path, String>> parseChecksumPairs(Path manifestFile) throws IOException {
        Pattern fileLinePattern = Pattern.compile(FILE_LINE_PATTERN);
        List<Pair<Path, String>> checksumPairs = new ArrayList<>();
        for (String line : Files.readAllLines(manifestFile)) {
            Matcher matcher = fileLinePattern.matcher(line);
            if (!matcher.find()) {
                log.warn("Unable to parse manifest line: " + line);
                continue;
            }
            checksumPairs.add(Pair.of(manifestFile.getParent().resolve(matcher.group(2).replace("\\","/")).normalize().toAbsolutePath(), matcher.group(1)))
            ;
        }
        return checksumPairs;
    }
}
