package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.BatchState;
import cz.cas.lib.arclib.domain.Sip;
import cz.cas.lib.arclib.domain.SipState;
import cz.cas.lib.arclib.store.BatchStore;
import cz.cas.lib.arclib.store.SipStore;
import cz.cas.lib.arclib.store.Transactional;
import cz.inqool.uas.exception.MissingObject;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static cz.inqool.uas.util.Utils.asList;
import static cz.inqool.uas.util.Utils.checked;
import static cz.inqool.uas.util.Utils.notNull;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;

@Slf4j
@Component
public class IngestBpmDelegate implements JavaDelegate {

    protected SipStore sipStore;
    protected BatchStore batchStore;
    protected String workspace;

    /**
     * Executes the ingest process for the given SIP:
     * 1. copies SIP to workspace
     * 2. processes SIP (...waits for a second)
     * 3. deletes SIP from workspace
     *
     * @param execution parameter containing the SIP id
     * @throws FileNotFoundException
     * @throws InterruptedException
     */
    @Transactional
    @Override
    public void execute(DelegateExecution execution) throws IOException, InterruptedException {
        String sipId = (String) execution.getVariable("sipId");
        String batchId = (String) execution.getVariable("batchId");

        log.info("BPM process for SIP " + sipId + " started.");

        try {
            Sip sip = sipStore.find(sipId);
            notNull(sip, () -> new MissingObject(Sip.class, sipId));

            String sipPath = sip.getPath();
            if (sipPath != null) {
                copySipToWorkspace(sipPath, sipId);

                log.info("SIP " + sipId + " has been successfully copied to workspace.");

                /*
                Here will come the processing of SIP.
                We use the thread sleep to simulate the time required to process the SIP.
                */
                Thread.sleep(1000);
                delSipFromWorkspace(sipId);
            }

        sip.setState(SipState.PROCESSED);
        sipStore.save(sip);
        log.info("SIP " + sipId + " has been processed. The SIP state changed to PROCESSED.");

        } finally {
            Batch batch = batchStore.find(batchId);
            notNull(batch, () -> new MissingObject(Batch.class, batchId));

            boolean allSipsProcessed = sipStore.findAllInList(asList(batch.getIds())).stream()
                    .allMatch(s -> s.getState() == SipState.PROCESSED ||
                                    s.getState() == SipState.FAILED);

            if (allSipsProcessed && batch.getState() == BatchState.PROCESSING) {
                batch.setState(BatchState.PROCESSED);
                batchStore.save(batch);
                log.info("Batch " + batchId + " has been processed. The batch state changed to PROCESSED.");
            }
        }
    }

    /**
     * Copies folder with SIP contents to workspace
     * @param src path to the folder where the SIP is located
     * @param sipId id of the SIP
     * @throws IOException
     */
    private void copySipToWorkspace(String src, String sipId) throws IOException {
        checked(() -> {
            Path folder = Paths.get(workspace);
            if (!exists(folder)) {
                createDirectories(folder);
            }

            FileSystemUtils.copyRecursively(new File(src), new File(workspace, sipId));
        });
    }

    /**
     * From the workspace folder deletes file with the provided id
     *
     * @param sipId id of the file to delete
     */
    private void delSipFromWorkspace(String sipId) throws IOException {
        Path directory = Paths.get(workspace, sipId);

        if (exists(directory)) {
            FileSystemUtils.deleteRecursively(new File(directory.toAbsolutePath().toString()));
        }
    }

    @Inject
    public void setSipStore(SipStore sipStore) {
        this.sipStore = sipStore;
    }

    @Inject
    public void setBatchStore(BatchStore batchStore) {
        this.batchStore = batchStore;
    }

    @Inject
    public void setWorkspace(@Value("${arclib.workspace}") String workspace) {
        this.workspace = workspace;
    }
}
