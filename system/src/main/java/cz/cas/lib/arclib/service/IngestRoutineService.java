package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.IngestRoutine;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domainbase.exception.MissingAttribute;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.IngestRoutineDto;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.IngestRoutineStore;
import cz.cas.lib.arclib.store.ProducerProfileStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.scheduling.job.Job;
import cz.cas.lib.core.scheduling.job.JobService;
import cz.cas.lib.core.script.ScriptType;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static cz.cas.lib.core.util.Utils.isNull;
import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class IngestRoutineService {
    private Resource batchStartScript;

    private IngestRoutineStore store;

    @Getter
    private ProducerProfileStore producerProfileStore;

    @Getter
    private JobService jobService;

    private UserDetails userDetails;

    private BeanMappingService beanMappingService;

    /**
     * Saves the instance of ingest routine to database. Furthermore it creates a job that performs the
     * scheduled tasks for the ingest routine.
     *
     * @param ingestRoutine ingest routine to be saved
     * @return saved ingest routine
     */
    @Transactional
    public IngestRoutine save(IngestRoutine ingestRoutine) {
        notNull(ingestRoutine.getJob(), () -> new MissingAttribute(IngestRoutine.class, ingestRoutine.getId(), "job"));
        User creator = new User();
        creator.setId(userDetails.getId());
        ingestRoutine.setCreator(creator);
        IngestRoutine existingRoutine = store.find(ingestRoutine.getId());
        if (existingRoutine != null) {
            ingestRoutine.getJob().setId(existingRoutine.getJob().getId());
        }
        createOrUpdateIngestRoutineJob(ingestRoutine);
        return store.save(ingestRoutine);
    }

    /**
     * Creates and stores a scheduled job that performs the tasks to be executed by the ingest routine.
     * The job is performed by the GROOVY script specified in <code>batchStartScript</code>. The script is assigned
     * parameters according to the values derived from the provided instance of <code>ingestRoutine</code>.
     *
     * @param ingestRoutine
     */
    private void createOrUpdateIngestRoutineJob(IngestRoutine ingestRoutine) {
        log.debug("Scheduling ingest routine job for ingest routine: " + ingestRoutine.getId());
        Job job = ingestRoutine.getJob();
        job.setScriptType(ScriptType.GROOVY);
        job.setName("Ingest routine");
        try {
            String script = StreamUtils.copyToString(batchStartScript.getInputStream(), Charset.defaultCharset());
            job.setScript(script);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        ProducerProfile producerProfile = ingestRoutine.getProducerProfile();
        notNull(producerProfile, () -> new MissingAttribute(IngestRoutine.class, ingestRoutine.getId(),
                "producerProfile"));

        producerProfile = producerProfileStore.find(producerProfile.getId());
        notNull(producerProfile, () -> new MissingObject(ProducerProfile.class, ingestRoutine.getProducerProfile().getId()));

        String transferAreaPath = ingestRoutine.getTransferAreaPath();
        notNull(transferAreaPath, () -> new MissingAttribute(IngestRoutine.class, ingestRoutine.getId(),
                "transferAreaPath"));

        String workflowConfig = ingestRoutine.getWorkflowConfig();
        notNull(workflowConfig, () -> new MissingAttribute(IngestRoutine.class, ingestRoutine.getId(),
                "workflowConfig"));

        User creator = ingestRoutine.getCreator();
        notNull(creator, () -> new MissingAttribute(IngestRoutine.class, ingestRoutine.getId(),
                "creator"));

        Map<String, String> jobParams = new HashMap<>();
        jobParams.put("creatorId", creator.getId());
        jobParams.put("externalId", producerProfile.getExternalId());
        jobParams.put("transferAreaPath", transferAreaPath);
        jobParams.put("workflowConfig", workflowConfig);
        jobParams.put("routineId", ingestRoutine.getId());
        job.setParams(jobParams);

        jobService.save(job);
    }

    /**
     * Gets instance of ingest routine by id.
     *
     * @param id id of the instance
     * @return the retrieved instance
     */
    public IngestRoutine find(String id) {
        IngestRoutine entity = store.find(id);
        notNull(entity, () -> new MissingObject(IngestRoutine.class, id));
        return entity;
    }

    /**
     * Lists all instances of ingest routine by the producer id
     *
     * @return list of ingest routine instances
     */
    public Collection<IngestRoutine> find() {
        if (ArclibUtils.hasRole(userDetails, Roles.SUPER_ADMIN)) {
            return store.findAll();
        } else {
            return store.findByProducerId(userDetails.getProducerId());
        }
    }

    /**
     * Deletes the instance of ingest routine from database.
     *
     * @param id id of the instance
     */
    @Transactional
    public void delete(String id) {
        IngestRoutine entity = store.find(id);
        Utils.notNull(entity, () -> new MissingObject(store.getType(), id));
        isNull(entity.getCurrentlyProcessingBatch(), () -> new ForbiddenException("Can't delete routine when there is unfinished batch: " + entity.getCurrentlyProcessingBatch().getId() + " which was created by this routine."));
        store.delete(entity);

        log.debug("Canceling ingest routine job for ingest routine: " + id + ".");
        jobService.delete(entity.getJob());
    }

    public Collection<IngestRoutineDto> listIngestRoutineDtos() {
        Collection<IngestRoutine> all = store.findAll();
        return beanMappingService.mapTo(all, IngestRoutineDto.class);
    }

    /**
     * Finds ingest routine with the given name
     *
     * @param name
     * @return ingest routine found, <code>null</code> otherwise
     */
    public IngestRoutine findByName(String name) {
        return store.findByName(name);
    }

    @Inject
    public void setBeanMappingService(BeanMappingService beanMappingService) {
        this.beanMappingService = beanMappingService;
    }

    @Inject
    public void setStore(IngestRoutineStore store) {
        this.store = store;
    }

    @Inject
    public void setJobService(JobService jobService) {
        this.jobService = jobService;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Inject
    public void setProducerProfileStore(ProducerProfileStore producerProfileStore) {
        this.producerProfileStore = producerProfileStore;
    }

    @Inject
    public void setBatchStartScript(@Value("${arclib.script.ingestRoutineBatchStart}") Resource batchStartScript) {
        this.batchStartScript = batchStartScript;
    }
}
