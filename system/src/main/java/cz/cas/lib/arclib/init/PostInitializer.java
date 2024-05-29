package cz.cas.lib.arclib.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenObject;
import cz.cas.lib.arclib.index.solr.IndexQueryUtils;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.authorization.role.UserRoleService;
import cz.cas.lib.arclib.service.BatchService;
import cz.cas.lib.arclib.service.IngestErrorHandler;
import cz.cas.lib.arclib.service.incident.CustomIncidentHandler;
import cz.cas.lib.arclib.store.AipBulkDeletionStore;
import cz.cas.lib.arclib.store.ProducerStore;
import cz.cas.lib.core.scheduling.job.Job;
import cz.cas.lib.core.scheduling.job.JobService;
import cz.cas.lib.core.script.ScriptType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StreamUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;

import static java.nio.file.Files.*;

@Component
@Slf4j
public class PostInitializer implements ApplicationListener<ContextRefreshedEvent> {
    @Inject
    private ObjectMapper objectMapper;
    @Value("${env:production}")
    private String env;
    @Inject
    private IngestErrorHandler ingestErrorHandler;
    @Inject
    private CustomIncidentHandler customIncidentHandler;
    @Value("${arclib.path.workspace}")
    private String workspace;
    @Inject
    private ProducerStore producerStore;
    @Inject
    private BatchService batchService;
    @Inject
    private ArclibMailCenter mailCenter;
    @Inject
    private UserRoleService assignedRoleService;
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
    @Value("${spring.servlet.multipart.location}")
    private String temporaryMultipartFilesLocation;
    @Value("${solr.maxRows}")
    private Integer solrMaxRows;
    @Inject
    private AipBulkDeletionStore aipBulkDeletionStore;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!env.equals("test")) {
            scheduleBatchBpmUndeployment();
            checkTransferAreasReachable();
            Collection<Job> jobs = jobService.findAll();
            log.debug("Starting jobs...");
            jobs.forEach(jobService::updateJobSchedule);
        }

        objectMapper.registerModule(new Hibernate5Module());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setDateFormat(new ISO8601DateFormat());
        objectMapper.findAndRegisterModules();
        //set manually, not injected because of circular dependency
        customIncidentHandler.setIngestErrorHandler(ingestErrorHandler);
        customIncidentHandler.setBatchService(batchService);

        createTemporaryMultipartFilesDirectory();
        if (solrMaxRows != null)
            IndexQueryUtils.solrMaxRows = solrMaxRows;

        setBulkDeletionRequestsToFail();
        log.debug("Arclib instance started successfully.");
    }

    /**
     * Checks reachability of transfer area and for every producer its respective transfer area
     */
    private void checkTransferAreasReachable() {
        log.debug("Checking reachability of transfer area.");
        if (!Files.exists(Paths.get(fileStorage))) {
            String message = "Transfer area is not reachable.";
            log.warn(message);
            assignedRoleService.getUsersWithPermission(Permissions.SUPER_ADMIN_PRIVILEGE).stream()
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
                assignedRoleService.getUsersWithPermission(Permissions.SUPER_ADMIN_PRIVILEGE).stream()
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

    public void setBulkDeletionRequestsToFail() {
        transactionTemplate.execute(t -> aipBulkDeletionStore.setAllRunningToFail());
    }


    /**
     * Create folder for temporary multipart files obtained from requests.
     * Path is declared in application.yml
     */
    private void createTemporaryMultipartFilesDirectory() {
        try {
            Path folder = Paths.get(temporaryMultipartFilesLocation);
            if (!isDirectory(folder) && exists(folder)) {
                throw new ForbiddenObject(Path.class, temporaryMultipartFilesLocation);
            } else if (!isDirectory(folder)) {
                createDirectories(folder);
                log.debug("Created folder for temporary multipart files.");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
