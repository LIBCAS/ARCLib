package cz.cas.lib.arclib.service.fixity;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.bpm.IngestTool;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
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

    private static final String FILE_LINE_PATTERN = "(\\w+)[*\\s]+(\\S+)";
    private static final Set<String> supportedChecksumTypes = Set.of("md5", "sha1", "sha256", "sha512");

    /**
     * Verifies fixities of all files listed in any checksum file located in SIP.
     *
     * @param pathToFixityFile redundant argument, the whole SIP is scanned for all common checksum files ({@link #supportedChecksumTypes})
     */
    @Override
    public void verifySIP(Path sipWsPath, Path pathToFixityFile, String externalId, JsonNode configRoot, Map<String, Pair<String, String>> formatIdentificationResult, int fixityCheckToolCounter, IngestTool fixityCheckerTool)
            throws IncidentException, IOException {
        Map<Path, List<Path>> missingFilesWrapper = new HashMap<>();
        Map<Path, List<Path>> invalidFixitiesWrapper = new HashMap<>();
        List<Path> filesWithChecksums = Files.walk(sipWsPath).filter(p -> supportedChecksumTypes.contains((FilenameUtils.getExtension(p.toFile().getName())))).collect(Collectors.toList());
        if (filesWithChecksums.isEmpty())
            return;
        log.debug("Found common checksum files, supported extensions: {}, found files: {}. Starting verification", Arrays.toString(supportedChecksumTypes.toArray()), Arrays.toString(filesWithChecksums.toArray()));
        for (Path checksumFile : filesWithChecksums) {
            log.debug("Verifying fixity of files specified in checksum file: {}", checksumFile);
            List<Path> missingFiles = new ArrayList<>();
            List<Path> invalidFixities = new ArrayList<>();
            List<Pair<Path, String>> checksumPairs = parseChecksumPairs(sipWsPath, checksumFile, missingFiles);
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

            for (Pair<Path, String> checksumPair : checksumPairs) {
                Path filePath = checksumPair.getLeft();
                byte[] computedChecksum = counter.computeDigest(filePath);
                if (!counter.checkIfDigestsMatches(checksumPair.getRight(), computedChecksum)) {
                    invalidFixities.add(filePath);
                }
            }
            if (!invalidFixities.isEmpty())
                invalidFixitiesWrapper.put(checksumFile, invalidFixities);
            if (!missingFiles.isEmpty())
                missingFilesWrapper.put(checksumFile, missingFiles);
        }
        if (!missingFilesWrapper.isEmpty())
            invokeMissingFilesIssue(sipWsPath, missingFilesWrapper, externalId, configRoot, fixityCheckToolCounter, fixityCheckerTool);
        if (!invalidFixitiesWrapper.isEmpty())
            invokeInvalidChecksumsIssue(sipWsPath, invalidFixitiesWrapper, externalId, configRoot, formatIdentificationResult, fixityCheckToolCounter, fixityCheckerTool);
        log.debug("Verification of common checksum files has ended.");
    }

    /**
     * Parses checksum pairs
     *
     * @param manifestFile file containing the fixities
     * @return list of pairs of file paths to checksum values
     * @throws IOException <code>manifestFile</code> could not be read
     */
    private List<Pair<Path, String>> parseChecksumPairs(Path sipWsPath, Path manifestFile, List<Path> missingFiles) throws IOException {
        Pattern fileLinePattern = Pattern.compile(FILE_LINE_PATTERN);
        List<Pair<Path, String>> checksumPairs = new ArrayList<>();
        for (String line : Files.readAllLines(manifestFile)) {
            Matcher matcher = fileLinePattern.matcher(line);
            if (!matcher.find()) {
                log.warn("Unable to parse manifest line: " + line);
                continue;
            }
            String parsedPath = matcher.group(2).replace("\\", "/");
            if (parsedPath.startsWith("/")) {
                Path absolutePath = sipWsPath.resolve(parsedPath.substring(1)).normalize().toAbsolutePath();
                if (absolutePath.toFile().isFile()) {
                    checksumPairs.add(Pair.of(absolutePath, matcher.group(1)));
                    continue;
                }
                Path fallbackRelativePath = manifestFile.getParent().resolve(parsedPath.substring(1)).normalize().toAbsolutePath();
                if (fallbackRelativePath.toFile().isFile()) {
                    checksumPairs.add(Pair.of(fallbackRelativePath, matcher.group(1)));
                    continue;
                }
                missingFiles.add(absolutePath);
            } else {
                Path relativePath = manifestFile.getParent().resolve(parsedPath).normalize().toAbsolutePath();
                if (relativePath.toFile().isFile())
                    checksumPairs.add(Pair.of(relativePath, matcher.group(1)));
                else
                    missingFiles.add(relativePath);
            }
        }
        return checksumPairs;
    }
}
