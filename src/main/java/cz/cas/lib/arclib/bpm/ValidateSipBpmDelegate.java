package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.Sip;
import cz.cas.lib.arclib.domain.SipState;
import cz.cas.lib.arclib.store.SipStore;
import cz.cas.lib.arclib.validation.ValidationService;
import cz.inqool.uas.exception.MissingAttribute;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static cz.inqool.uas.util.Utils.checked;
import static cz.inqool.uas.util.Utils.notNull;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;

@Slf4j
@Component
public class ValidateSipBpmDelegate implements JavaDelegate {

    protected SipStore sipStore;
    protected String workspace;
    protected ValidationService service;

    /**
     * Executes the validation process for the given SIP:
     * 1. copies SIP to workspace
     * 2. validates SIP
     * 3. deletes SIP from workspace
     *
     * @param execution parameter containing the SIP id
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws XPathExpressionException
     */
    @Transactional
    @Override
    public void execute(DelegateExecution execution) throws IOException, ParserConfigurationException,
            SAXException, XPathExpressionException {
        String sipId = (String) execution.getVariable("sipId");
        String validationProfileId = (String) execution.getVariable("validationProfileId");

        log.info("BPM process for SIP " + sipId + " started.");

        Sip sip = sipStore.find(sipId);
        notNull(sip, () -> new MissingObject(Sip.class, sipId));

        String sipPath = sip.getPath();
        notNull(sipPath, () -> new MissingAttribute(Sip.class, "sipPath"));

        copySipToWorkspace(sipPath, sipId);
        log.info("SIP " + sipId + " has been successfully copied to workspace.");

        String workspaceSipPath = workspace + "/" + sipId;
        service.validateSip(sipId, workspaceSipPath, validationProfileId);

        sip.setState(SipState.PROCESSED);
        sipStore.save(sip);
        log.info("SIP " + sipId + " has been processed. The SIP state changed to PROCESSED.");

        delSipFromWorkspace(sipId);
    }

    /**
     * Copies folder with SIP contents to workspace
     * @param src path to the folder where the SIP is located
     * @param sipId id of the SIP
     */
    private void copySipToWorkspace(String src, String sipId) {
        checked(() -> {
            Path folder = Paths.get(workspace);
            if (!exists(folder)) {
                createDirectories(folder);
            }

            FileSystemUtils.copyRecursively(new File(src), new File(workspace + "/" + sipId));
        });
    }

    /**
     * From the workspace deletes the folder with the data for the respective SIP
     *
     * @param sipId id of the file to delete
     */
    private void delSipFromWorkspace(String sipId) {
        Path path = Paths.get(workspace, sipId);

        if (exists(path)) {
            checked(() -> FileSystemUtils.deleteRecursively(new File(workspace + "/" + sipId)));
        } else {
            log.warn("File {} not found.", path);
        }
    }

    @Inject
    public void setSipStore(SipStore sipStore) {
        this.sipStore = sipStore;
    }

    @Inject
    public void setWorkspace(@Value("${arclib.workspace}") String workspace) {
        this.workspace = workspace;
    }

    @Inject
    public void setService(ValidationService service) {
        this.service = service;
    }
}
