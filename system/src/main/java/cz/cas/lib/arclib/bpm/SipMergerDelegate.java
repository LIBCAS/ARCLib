package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageException;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageResponseExtractor;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageService;
import cz.cas.lib.arclib.service.fixity.FixityCounterFacade;
import cz.cas.lib.arclib.store.SipStore;
import cz.cas.lib.arclib.utils.ZipUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.dom4j.DocumentException;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static cz.cas.lib.core.util.Utils.bytesToHexString;

@Slf4j
@Service
public class SipMergerDelegate extends ArclibDelegate {

    @Getter
    private String toolName = "ARCLib_SIP_merger";

    private SipStore sipStore;
    private ArchivalStorageService archivalStorageService;
    private FixityCounterFacade fixityCounterFacade;
    private ArchivalStorageResponseExtractor archivalStorageResponseExtractor;

    @Override
    public void executeArclibDelegate(DelegateExecution execution) throws TransformerException, IOException, ParserConfigurationException, SAXException, DocumentException, ArchivalStorageException {
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
            Path previousVersionAipDataDir = archivalStorageResponseExtractor.extractAipAsFolderWithXmlsBySide(new ZipInputStream(archivalStorageResponse), previousVersionSip.getId(), previousVersionAipUnpackedInWorkspace);

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

        ingestEventStore.save(new IngestEvent(ingestWorkflowService.findByExternalId(ingestWorkflowExternalId), toolService.getByNameAndVersion(getToolName(), getToolVersion()), true, null));
    }

    @Inject
    public void setSipStore(SipStore sipStore) {
        this.sipStore = sipStore;
    }

    @Inject
    public void setFixityCounterFacade(FixityCounterFacade fixityCounterFacade) {
        this.fixityCounterFacade = fixityCounterFacade;
    }

    @Inject
    public void setArchivalStorageService(ArchivalStorageService archivalStorageService) {
        this.archivalStorageService = archivalStorageService;
    }

    @Inject
    public void setArchivalStorageResponseExtractor(ArchivalStorageResponseExtractor archivalStorageResponseExtractor) {
        this.archivalStorageResponseExtractor = archivalStorageResponseExtractor;
    }
}
