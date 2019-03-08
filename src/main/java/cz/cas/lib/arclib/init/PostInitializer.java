package cz.cas.lib.arclib.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import cz.cas.lib.arclib.index.solr.SolrReindexService;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.security.authorization.assign.AssignedRoleService;
import cz.cas.lib.arclib.service.IngestErrorHandler;
import cz.cas.lib.arclib.service.incident.CustomIncidentHandler;
import cz.cas.lib.arclib.service.preservationPlanning.FormatLibraryUpdater;
import cz.cas.lib.arclib.store.ProducerStore;
import cz.cas.lib.core.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

@Component
@Slf4j
public class PostInitializer implements ApplicationListener<ApplicationReadyEvent> {
    @Inject
    private DataSource ds;
    @Inject
    private ObjectMapper objecMapper;
    @Inject
    private SolrReindexService reindexService;
    @Value("${env}")
    private String env;
    @Inject
    private IngestErrorHandler ingestErrorHandler;
    @Inject
    private CustomIncidentHandler customIncidentHandler;
    @Inject
    private SolrTestRecordsInitializer solrTestRecordsInitializer;
    @Value("${arclib.path.workspace}")
    private String workspace;
    @Inject
    private RepositoryService repositoryService;
    @Inject
    private RuntimeService runtimeService;
    @Inject
    private FormatLibraryUpdater formatLibraryUpdater;
    @Inject
    private ProducerStore producerStore;
    @Inject
    private ArclibMailCenter mailCenter;
    @Inject
    private AssignedRoleService assignedRoleService;
    @Value("${arclib.path.fileStorage}")
    private String fileStorage;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent events) {
        objecMapper.registerModule(new Hibernate5Module());
        objecMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objecMapper.setDateFormat(new ISO8601DateFormat());
        //error handler is set manually, not injected because of circular dependency
        customIncidentHandler.setIngestErrorHandler(ingestErrorHandler);
        if (env.equals("staging")) {
            try {
                initializeWithTestData();
            } catch (Exception e) {
                throw new GeneralException("Data init error", e);
            }
        }
        if (!env.equals("test")) {
            formatLibraryUpdater.scheduleFormatLibraryUpdates();
            checkTransferAreasReachable();
        }
        log.debug("Arclib instance started successfully.");
    }

    public void initializeWithTestData() throws IOException, SQLException {
        runtimeService.createProcessInstanceQuery().list().forEach(p -> runtimeService.deleteProcessInstance(p.getProcessInstanceId(), "cleanup"));
        repositoryService.createDeploymentQuery().list().forEach(d -> repositoryService.deleteDeployment(d.getId(), true));
        cleanWorkspace();
        log.debug("Workspace clean up successful");
        sqlTestInit();
        reindexService.refreshAll();
        solrTestRecordsInitializer.init();
        log.debug("Data init successful");
    }

    private void sqlTestInit() throws SQLException, IOException {
        try (Connection con = ds.getConnection()) {
            ScriptRunner runner = new ScriptRunner(con, false, true);
            runner.runScript(new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("init.sql"))));
            runner.runScript(new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("formats.sql"))));
        }
    }

    /**
     * Deletes the workspace folder (workspace folder is recreated later)
     */
    private void cleanWorkspace() {
        FileSystemUtils.deleteRecursively(new File(workspace));
    }

    /**
     * Checks reachability of transfer area and for every producer its respective transfer area
     */
    private void checkTransferAreasReachable() {
        log.debug("Checking reachability of transfer area.");
        if (!Files.exists(Paths.get(fileStorage))) {
            String message = "Transfer area is not reachable.";
            log.warn(message);
            assignedRoleService.getUsersWithRole(Roles.SUPER_ADMIN).stream()
                    .forEach(user -> mailCenter.sendTransferAreaNotReachableNotification(user.getEmail(), message, Instant.now()));
            return;
        }

        log.debug("Checking reachability of transfer areas for every producer.");
        producerStore.findAll().forEach(producer -> {
                Path fullTransferAreaPath = Paths.get(fileStorage, producer.getTransferAreaPath());
                if (!Files.exists(fullTransferAreaPath)) {
                    String message = "Transfer area at " + fullTransferAreaPath + " of producer " +
                            producer.getName() + " is not reachable.";
                    log.warn(message);
                    assignedRoleService.getUsersWithRole(Roles.SUPER_ADMIN).stream()
                            .forEach(user -> mailCenter.sendTransferAreaNotReachableNotification(user.getEmail(), message, Instant.now()));
                }
            });
    }
}
