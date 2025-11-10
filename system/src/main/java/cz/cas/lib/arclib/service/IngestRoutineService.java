package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.IngestRoutine;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingAttribute;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.IngestRoutineDto;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.exception.ReingestInProgressException;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.IngestRoutineStore;
import cz.cas.lib.arclib.store.ProducerProfileStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.scheduling.JobRunner;
import cz.cas.lib.core.scheduling.job.Job;
import cz.cas.lib.core.scheduling.job.JobService;
import cz.cas.lib.core.script.ScriptType;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.*;

@Slf4j
@Service
public class IngestRoutineService {

    private IngestRoutineStore store;
    private ProducerProfileStore producerProfileStore;
    private JobService jobService;
    private BeanMappingService beanMappingService;
    private UserDetails userDetails;
    private Resource batchStartScript;
    private JobRunner jobRunner;

    public void run(String id) {
        IngestRoutine routine = store.find(id);
        notNull(routine, () -> new MissingObject(IngestRoutine.class, id));
        eq(routine.isReingest(), true, () -> new BadArgument("Routine is not related to reingest"));
        jobRunner.run(routine.getJob());
    }

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
            eq(ingestRoutine.isAuto(), existingRoutine.isAuto(), () -> new BadArgument("Field 'auto' is not allowed to be updated."));
            ingestRoutine.getJob().setId(existingRoutine.getJob().getId());
        }
        createOrUpdateIngestRoutineJob(ingestRoutine);
        return store.save(ingestRoutine);
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
     * Deletes the instance of ingest routine from database.
     *
     * @param id id of the instance
     */
    @Transactional
    public void delete(String id) {
        IngestRoutine entity = store.findWithBatchesFilled(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));
        isEmpty(entity.getCurrentlyProcessingBatches(), () -> new ForbiddenException("Can't delete routine when there is unfinished batch: " + entity.getCurrentlyProcessingBatches().get(0) + " which was created by this routine."));
        store.delete(entity);

        log.debug("Canceling ingest routine job for ingest routine: " + id + ".");
        jobService.delete(entity.getJob());
    }

    public void deleteReingestRoutines() throws ReingestInProgressException {
        List<IngestRoutine> routines = store.getReingestRoutines();

        Set<Batch> processingBatches = routines.stream().flatMap(r -> r.getCurrentlyProcessingBatches().stream()).collect(Collectors.toSet());
        if (!processingBatches.isEmpty()) {
            throw new ReingestInProgressException("can't delete reingest routines since there are some unfinished batches: " + processingBatches);
        }
        for (IngestRoutine r : routines) {
            delete(r.getId());
        }
    }

    public List<IngestRoutine> getReingestRoutines() {
        return store.getReingestRoutines();
    }

    @Transactional
    public Collection<IngestRoutineDto> listIngestRoutineDtos() {
        Collection<IngestRoutine> all = this.findFilteredByProducer();
        return beanMappingService.mapTo(all, IngestRoutineDto.class);
    }

    /**
     * Creates and stores a scheduled job that performs the tasks to be executed by the ingest routine.
     * The job is performed by the GROOVY script specified in <code>batchStartScript</code>. The script is assigned
     * parameters according to the values derived from the provided instance of <code>ingestRoutine</code>.
     */
    private void createOrUpdateIngestRoutineJob(IngestRoutine ingestRoutine) {
        log.debug("Scheduling ingest routine job for ingest routine: " + ingestRoutine.getId());

        notNull(ingestRoutine.getProducerProfile(), () -> new MissingAttribute(IngestRoutine.class, ingestRoutine.getId(), "producerProfile"));

        ProducerProfile producerProfile = producerProfileStore.find(ingestRoutine.getProducerProfile().getId());
        notNull(producerProfile, () -> new MissingObject(ProducerProfile.class, ingestRoutine.getProducerProfile().getId()));

        notNull(producerProfile.getProducer(), () -> new MissingAttribute(ProducerProfile.class, producerProfile.getId(), "producer"));

        if (!ingestRoutine.isReingest()) {
            notNull(ingestRoutine.getTransferAreaPath(), () -> new MissingAttribute(IngestRoutine.class, ingestRoutine.getId(), "transferAreaPath"));

            notNull(ingestRoutine.getWorkflowConfig(), () -> new MissingAttribute(IngestRoutine.class, ingestRoutine.getId(), "workflowConfig"));
            if (ingestRoutine.getWorkflowConfig().isEmpty())
                throw new BadArgument("Workflow config is empty. Try to use '{}'");
        }

        notNull(ingestRoutine.getCreator(), () -> new MissingAttribute(IngestRoutine.class, ingestRoutine.getId(), "creator"));

        Job job = ingestRoutine.getJob();
        job.setScriptType(ScriptType.GROOVY);
        job.setName("Ingest routine");
        try {
            String script = StreamUtils.copyToString(batchStartScript.getInputStream(), Charset.defaultCharset());
            job.setScript(script);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Map<String, String> jobParams = new HashMap<>();
        jobParams.put("routineId", ingestRoutine.getId());
        job.setParams(jobParams);

        jobService.save(job);
    }

    /**
     * Lists all instances of ingest routine by the producer id
     *
     * @return list of ingest routine instances
     */
    private Collection<IngestRoutine> findFilteredByProducer() {
        if (ArclibUtils.hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            return store.findAll();
        } else {
            return store.findByProducerId(userDetails.getProducerId());
        }
    }


    @Autowired
    public void setBeanMappingService(BeanMappingService beanMappingService) {
        this.beanMappingService = beanMappingService;
    }

    @Autowired
    public void setStore(IngestRoutineStore store) {
        this.store = store;
    }

    @Autowired
    public void setJobService(JobService jobService) {
        this.jobService = jobService;
    }

    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Autowired
    public void setProducerProfileStore(ProducerProfileStore producerProfileStore) {
        this.producerProfileStore = producerProfileStore;
    }

    @Autowired
    public void setBatchStartScript(@Value("${arclib.script.ingestRoutineBatchStart}") Resource batchStartScript) {
        this.batchStartScript = batchStartScript;
    }

    @Autowired
    public void setJobRunner(JobRunner jobRunner) {
        this.jobRunner = jobRunner;
    }
}
