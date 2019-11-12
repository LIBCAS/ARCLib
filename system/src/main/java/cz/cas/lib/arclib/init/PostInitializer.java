package cz.cas.lib.arclib.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.index.solr.ReindexService;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.security.authorization.assign.AssignedRoleService;
import cz.cas.lib.arclib.service.BatchService;
import cz.cas.lib.arclib.service.IngestErrorHandler;
import cz.cas.lib.arclib.service.incident.CustomIncidentHandler;
import cz.cas.lib.arclib.store.ProducerStore;
import cz.cas.lib.core.scheduling.job.Job;
import cz.cas.lib.core.scheduling.job.JobService;
import cz.cas.lib.core.script.ScriptType;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StreamUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;

@Component
@Slf4j
public class PostInitializer implements ApplicationListener<ContextRefreshedEvent> {
    @Inject
    private DataSource ds;
    @Inject
    private ObjectMapper objectMapper;
    @Inject
    private ReindexService reindexService;
    @Value("${env}")
    private String env;
    @Value("${jmsCommunicationRole}")
    private String jmsCommunicationRole;
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
    private ProducerStore producerStore;
    @Inject
    private BatchService batchService;
    @Inject
    private ArclibMailCenter mailCenter;
    @Inject
    private AssignedRoleService assignedRoleService;
    @Value("${arclib.path.fileStorage}")
    private String fileStorage;
    @Value("${arclib.cron.bpmDefUndeploy}")
    private String bpmDefUndeployCron;
    @Value("${arclib.script.bpmDefUndeploy}")
    private Resource bpmDefUndeployScript;
    @Inject
    private JobService jobService;
    @Inject
    private TransactionTemplate transactionTemplate;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (jmsCommunicationRole.equals("coordinator")) {
            if (env.equals("staging")) {
                try {
                    initializeWithTestData();
                } catch (Exception e) {
                    throw new GeneralException("Data init error", e);
                }
            }
            if (!env.equals("test")) {
                scheduleBatchBpmUndeployment();
                checkTransferAreasReachable();
                Collection<Job> jobs = jobService.findAll();
                log.debug("Starting jobs...");
                jobs.forEach(jobService::updateJobSchedule);
            }
        }

        objectMapper.registerModule(new Hibernate5Module());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setDateFormat(new ISO8601DateFormat());
        objectMapper.findAndRegisterModules();
        //set manually, not injected because of circular dependency
        customIncidentHandler.setIngestErrorHandler(ingestErrorHandler);
        customIncidentHandler.setBatchService(batchService);

        log.debug("Arclib instance started successfully.");
    }

    public void initializeWithTestData() throws IOException, SQLException {
        runtimeService.createProcessInstanceQuery().list().forEach(p -> runtimeService.deleteProcessInstance(p.getProcessInstanceId(), "cleanup"));
        repositoryService.createDeploymentQuery().list().forEach(d -> repositoryService.deleteDeployment(d.getId(), true));
        cleanWorkspace();
        log.debug("Workspace clean up successful");
        sqlTestInit();
        reindexService.dropReindexAll();
        solrTestRecordsInitializer.init();
        log.debug("Data init successful");
    }

    private void sqlTestInit() throws SQLException, IOException {
        try (Connection con = ds.getConnection()) {
            ScriptRunner runner = new ScriptRunner(con, false, true);
            runner.runScript(new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("sql/arclibDelete.sql"))));
            runner.runScript(new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("sql/formatLibraryDelete.sql"))));
            runner.runScript(new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("sql/formatLibraryInit.sql"))));
            runner.runScript(new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("sql/arclibInit.sql"))));
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

    private void scheduleBatchBpmUndeployment() {
        log.debug("Scheduling bpm definition undeploy script.");

        Job job = new Job();
        job.setId("0efaa744-5e81-4e69-a21f-6c5cfe5a8e53");
        job.setTiming(bpmDefUndeployCron);
        job.setName("Bpm definition undeploy.");
        job.setParams(new HashMap<>());
        job.setScriptType(ScriptType.GROOVY);
        try {
            String script = StreamUtils.copyToString(bpmDefUndeployScript.getInputStream(), Charset.defaultCharset());
            job.setScript(script);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        job.setActive(true);
        transactionTemplate.execute(status -> jobService.save(job));
    }
}
