package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import cz.cas.lib.arclib.exception.bpm.ConfigParserException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.antivirus.Antivirus;
import cz.cas.lib.arclib.service.antivirus.AntivirusType;
import cz.cas.lib.arclib.service.antivirus.ClamAntivirus;
import cz.cas.lib.arclib.store.IngestIssueStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static cz.cas.lib.arclib.utils.ArclibUtils.parseEnumFromConfig;

@Slf4j
@Service
public class AntivirusDelegate extends ArclibDelegate implements JavaDelegate {

    public static final String CONFIG_INFECTED_SIP_ACTION = "/antivirus/infectedSipAction";
    private Antivirus antivirus;
    private IngestIssueStore ingestIssueStore;
    private IngestWorkflowStore ingestWorkflowStore;
    private Path quarantinePath;

    /**
     * Scans SIP package for viruses.
     *
     * @throws IncidentException if the config cannot be parsed
     */
    @Override
    public void execute(DelegateExecution execution) throws IncidentException, FileNotFoundException {
        String ingestWorkflowExternalId = getIngestWorkflowExternalId(execution);
        log.info("Execution of Antivirus delegate started for ingest workflow " + ingestWorkflowExternalId + ".");
        JsonNode configRoot = getConfigRoot(execution);

        Path sipPath = ArclibUtils.getIngestWorkflowWorkspacePath(ingestWorkflowExternalId, workspace);
        antivirus = initialize(configRoot);
        antivirus.scan(sipPath, ingestWorkflowExternalId, configRoot);

        execution.setVariable(BpmConstants.VirusCheck.toolName, AntivirusType.CLAMAV.toString());
        execution.setVariable(BpmConstants.VirusCheck.toolVersion, antivirus.getToolVersion());

        execution.setVariable(BpmConstants.VirusCheck.success, true);
        execution.setVariable(BpmConstants.VirusCheck.dateTime,
                Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        log.info("Execution of Antivirus delegate finished for ingest workflow " + ingestWorkflowExternalId + ".");
    }

    public Antivirus initialize(JsonNode root) throws ConfigParserException {
        ObjectMapper om = new ObjectMapper();
        AntivirusType avType = parseEnumFromConfig(root, "/antivirus/type", AntivirusType.class);
        switch (avType) {
            case CLAMAV:
                String cmdExpr = "/antivirus/cmd";
                JsonNode cmdNode = root.at(cmdExpr);
                if (cmdNode.getNodeType() != JsonNodeType.ARRAY || cmdNode.size() < 1)
                    throw new ConfigParserException(cmdExpr, cmdNode.toString(), "Antivirus executable, with full path if not in $PATH variable, with switches");
                String[] cmd = ((List<String>) om.convertValue(cmdNode, List.class)).toArray(new String[0]);

                return new ClamAntivirus(cmd, ingestIssueStore, ingestWorkflowStore, quarantinePath);
            default:
                throw new GeneralException("unexpected antivirus scanner configuration error");
        }
    }

    @Inject
    public void setIngestWorkflowStore(IngestWorkflowStore ingestWorkflowStore) {
        this.ingestWorkflowStore = ingestWorkflowStore;
    }

    @Inject
    public void setIngestIssueStore(IngestIssueStore ingestIssueStore) {
        this.ingestIssueStore = ingestIssueStore;
    }

    @Inject
    public void setQuarantinePath(@Value("${arclib.path.quarantine}") String quarantinePath) {
        this.quarantinePath = Paths.get(quarantinePath);
    }
}
