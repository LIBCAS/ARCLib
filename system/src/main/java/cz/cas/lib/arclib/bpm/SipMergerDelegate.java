package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.exception.bpm.ConfigParserException;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageException;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageResponseExtractor;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageService;
import cz.cas.lib.arclib.service.fixity.FixityCounterFacade;
import cz.cas.lib.arclib.store.SipStore;
import cz.cas.lib.arclib.utils.ZipUtils;
import cz.cas.lib.core.util.SetUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static cz.cas.lib.core.util.Utils.bytesToHexString;

@Slf4j
@Service
public class SipMergerDelegate extends ArclibDelegate {

    @Getter
    private String toolName = "ARCLib_SIP_merger";
    public static final String MOVE_JSON_EXPR = "/sipmerger/move";
    public static final String REDUCE_JSON_EXPR = "/sipmerger/reduce";

    private SipStore sipStore;
    private ArchivalStorageService archivalStorageService;
    private FixityCounterFacade fixityCounterFacade;
    private ArchivalStorageResponseExtractor archivalStorageResponseExtractor;

    @Override
    public void executeArclibDelegate(DelegateExecution execution) throws TransformerException, IOException, ParserConfigurationException, SAXException, DocumentException, ArchivalStorageException, ConfigParserException {
        String sipId = getStringVariable(execution, BpmConstants.ProcessVariables.sipId);
        Sip sip = sipStore.find(sipId);
        Sip previousVersionSip = sip.getPreviousVersionSip();
        if (previousVersionSip != null) {
            Path sipUnpackedInWorkspace = getSipFolderWorkspacePath(execution);
            Path previousVersionAipUnpackedInWorkspace = sipUnpackedInWorkspace.resolveSibling("previous_version");
            if (previousVersionAipUnpackedInWorkspace.toFile().exists()) {
                FileUtils.deleteDirectory(previousVersionAipUnpackedInWorkspace.toFile());
            }

            InputStream archivalStorageResponse = archivalStorageService.exportSingleAip(previousVersionSip.getId(), false, null);
            Path previousVersionAipDataDir = archivalStorageResponseExtractor.extractAipAsFolderWithXmlsBySide(new ZipInputStream(archivalStorageResponse), previousVersionSip.getId(), previousVersionAipUnpackedInWorkspace, sipUnpackedInWorkspace.getFileName().toString());
            JsonNode configRoot = getConfigRoot(execution);

            //move
            JsonNode moveConfig = configRoot.at(MOVE_JSON_EXPR);
            MoveSpec[] moveSpecList = null;
            if (!moveConfig.isMissingNode()) {
                try {
                    moveSpecList = objectMapper.treeToValue(moveConfig, MoveSpec[].class);
                } catch (JsonProcessingException e) {
                    throw new ConfigParserException(MOVE_JSON_EXPR, moveConfig.toString(), "list of string pairs [{regex,replacement},{...}]");
                }
            }
            if (moveSpecList != null) {
                Map<String, String> oldToNewPathMap = new HashMap<>();
                MoveSpec[] finalMoveSpecList = moveSpecList;
                Files.walk(previousVersionAipDataDir).skip(1).filter(f -> f.toFile().isFile()).forEach(f -> {
                    String oldRelativePath = previousVersionAipDataDir.relativize(f).toString();
                    for (MoveSpec moveSpec : finalMoveSpecList) {
                        Matcher matcher = moveSpec.getCompiledRegex().matcher(oldRelativePath);
                        if (matcher.matches()) {
                            String newRelativePath = matcher.replaceAll(moveSpec.getReplacement());
                            oldToNewPathMap.put(oldRelativePath, newRelativePath);
                            break;
                        }
                    }
                });
                for (Map.Entry<String, String> mv : oldToNewPathMap.entrySet()) {
                    Path newPath = previousVersionAipDataDir.resolve(mv.getValue());
                    Files.createDirectories(newPath.getParent());
                    Files.move(previousVersionAipDataDir.resolve(mv.getKey()), newPath);
                }
            }

            //reduce
            JsonNode reductionConfig = configRoot.at(REDUCE_JSON_EXPR);
            ReductionSpec reductionSpec = null;
            if (!reductionConfig.isMissingNode()) {
                try {
                    reductionSpec = objectMapper.treeToValue(reductionConfig, ReductionSpec.class);
                } catch (JsonProcessingException e) {
                    throw new ConfigParserException(REDUCE_JSON_EXPR, reductionConfig.toString(), "regexes and reduction mode {regexes:[...],mode:DELETE/KEEP}");
                }
            }
            if (reductionSpec != null) {
                Set<String> allNodes = new HashSet<>();
                Set<String> matchedNodes = new HashSet<>();
                Set<String> nodesToDelete;
                for (String regex : reductionSpec.getRegexes()) {
                    Pattern compiledRegex = Pattern.compile(regex);
                    ReductionSpec finalReductionSpec = reductionSpec;
                    Files.walk(previousVersionAipDataDir).skip(1).forEach(fileOrDir -> {
                        Path relativePath = previousVersionAipDataDir.relativize(fileOrDir);
                        allNodes.add(relativePath.toString());
                        if (compiledRegex.matcher(relativePath.toString()).matches()) {
                            matchedNodes.add(relativePath.toString());
                            if (finalReductionSpec.getMode() == ReductionMode.KEEP) {
                                Path parentPath = relativePath.getParent();
                                while (parentPath != null) {
                                    matchedNodes.add(parentPath.toString());
                                    parentPath = parentPath.getParent();
                                }
                            }
                        }
                    });
                }
                switch (reductionSpec.getMode()) {
                    case DELETE:
                        nodesToDelete = matchedNodes;
                        break;
                    case KEEP:
                        nodesToDelete = SetUtils.difference(allNodes, matchedNodes);
                        break;
                    default:
                        throw new IllegalArgumentException("unsupported deletion mode");
                }
                for (String nodeToDelete : nodesToDelete) {
                    FileSystemUtils.deleteRecursively(previousVersionAipDataDir.resolve(nodeToDelete));
                }
            }

            //write
            FileUtils.copyDirectory(sipUnpackedInWorkspace.toFile(), previousVersionAipDataDir.toFile());


            Path sipZipInWorkspace = getSipZipPath(execution);
            Path mergedZipInWorkspace = previousVersionAipUnpackedInWorkspace.resolve(sipZipInWorkspace.getFileName());
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(mergedZipInWorkspace.toFile()))) {
                ZipUtils.zipFile(previousVersionAipDataDir.toFile(), previousVersionAipDataDir.getFileName().toString(), zos);
            }

            for (Hash h : sip.getHashes()) {
                h.setHashValue(bytesToHexString(fixityCounterFacade.computeDigest(h.getHashType(), mergedZipInWorkspace)));
            }
            sipStore.save(sip);

            FileUtils.deleteDirectory(sipUnpackedInWorkspace.toFile());
            Files.move(previousVersionAipDataDir, sipUnpackedInWorkspace);
            Files.move(mergedZipInWorkspace, sipZipInWorkspace, StandardCopyOption.REPLACE_EXISTING);
            FileUtils.deleteDirectory(previousVersionAipUnpackedInWorkspace.toFile());
        } else {
            log.info("there is no previous version of this ({}) SIP, nothing to merge, finishing with no action", sip.getId());
        }

        ingestEventStore.save(new IngestEvent(ingestWorkflowService.findByExternalId(getIngestWorkflowExternalId(execution)), toolService.getByNameAndVersion(getToolName(), getToolVersion()), true, null));
    }

    private static class MoveSpec {
        @Getter
        private String regex;
        @Getter
        private String replacement;
        private Pattern compiledRegex;

        public Pattern getCompiledRegex() {
            if (compiledRegex == null) {
                compiledRegex = Pattern.compile(regex);
            }
            return compiledRegex;
        }
    }

    private static class ReductionSpec {
        @Getter
        private List<String> regexes = new ArrayList<>();
        @Getter
        private ReductionMode mode;
    }

    public enum ReductionMode {
        DELETE, KEEP
    }

    @Autowired
    public void setSipStore(SipStore sipStore) {
        this.sipStore = sipStore;
    }

    @Autowired
    public void setFixityCounterFacade(FixityCounterFacade fixityCounterFacade) {
        this.fixityCounterFacade = fixityCounterFacade;
    }

    @Autowired
    public void setArchivalStorageService(ArchivalStorageService archivalStorageService) {
        this.archivalStorageService = archivalStorageService;
    }

    @Autowired
    public void setArchivalStorageResponseExtractor(ArchivalStorageResponseExtractor archivalStorageResponseExtractor) {
        this.archivalStorageResponseExtractor = archivalStorageResponseExtractor;
    }
}
