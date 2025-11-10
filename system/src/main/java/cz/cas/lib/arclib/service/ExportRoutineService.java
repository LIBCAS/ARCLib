package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domain.export.ExportRoutine;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenOperation;
import cz.cas.lib.arclib.domainbase.exception.MissingAttribute;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.AipQueryStore;
import cz.cas.lib.arclib.store.ExportRoutineStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.scheduling.job.Job;
import cz.cas.lib.core.scheduling.job.JobService;
import cz.cas.lib.core.script.ScriptType;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class ExportRoutineService {
    private Resource exportScript;

    private UserDetails userDetails;

    private ExportRoutineStore store;

    @Getter
    private JobService jobService;

    @Getter
    private AipQueryStore aipQueryStore;

    /**
     * Saves the instance of export routine to database. Furthermore it creates a job that performs the
     * scheduled tasks for the export routine.
     *
     * @param exportRoutine export routine to be saved
     * @return saved export routine
     */
    @Transactional
    public ExportRoutine save(ExportRoutine exportRoutine) {
        User user = userDetails.getUser();
        ExportRoutine oldExportRoutine = store.find(exportRoutine.getId());
        if (oldExportRoutine != null) {
            // if user is not SUPER_ADMIN then change of producer is forbidden
            if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
                if (oldExportRoutine.getCreator() != null && oldExportRoutine.getCreator().getProducer() != null)
                    eq(oldExportRoutine.getCreator().getProducer().getId(), user.getProducer().getId(), () -> new ForbiddenOperation("Cannot change ExportRoutine's Producer"));
            }
        }

        User creator = new User();
        creator.setId(userDetails.getId());
        exportRoutine.setCreator(creator);

        boolean userCanExportToSelectedFolder = Utils.pathIsNestedInParent(exportRoutine.getConfig().getExportFolder(), user.getExportFolders());
        Utils.eq(userCanExportToSelectedFolder, true, () -> new ForbiddenException("User is not allowed to export to folder: " + exportRoutine.getConfig().getExportFolder()
                + " allowed export folders for this user: " + String.join(",", user.getExportFolders())));

        log.debug("Scheduling aip export routine job for export routine: " + exportRoutine.getId());

        Job job = new Job();
        job.setActive(true);
        job.setTiming(Utils.toCron(exportRoutine.getExportTime()));
        job.setScriptType(ScriptType.GROOVY);
        try {
            String script = StreamUtils.copyToString(exportScript.getInputStream(), Charset.defaultCharset());
            job.setScript(script);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        AipQuery aipQuery = exportRoutine.getAipQuery();
        notNull(aipQuery, () -> new MissingAttribute(ExportRoutine.class, exportRoutine.getId(),
                "aipQuery"));

        aipQuery = aipQueryStore.find(aipQuery.getId());
        notNull(aipQuery, () -> new MissingObject(AipQuery.class, exportRoutine.getAipQuery().getId()));

        Map<String, String> jobParams = new HashMap<>();
        jobParams.put("aipQueryId", aipQuery.getId());
        jobParams.put("jobId", job.getId());
        job.setParams(jobParams);
        jobService.save(job);
        exportRoutine.setJob(job);

        if (oldExportRoutine != null && oldExportRoutine.getJob() != null) {
            jobService.delete(oldExportRoutine.getJob());
        }

        return store.save(exportRoutine);
    }

    /**
     * Gets instance of export routine by id.
     *
     * @param id id of the instance
     * @return the retrieved instance
     */
    @Transactional
    public ExportRoutine find(String id) {
        ExportRoutine entity = store.find(id);
        notNull(entity, () -> new MissingObject(ExportRoutine.class, id));
        return entity;
    }

    /**
     * Lists all instances of export routine
     *
     * @return list of export routine instances
     */
    @Transactional
    public Collection<ExportRoutine> find() {
        if (ArclibUtils.hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            return store.findAll();
        } else {
            return store.findByProducerId(userDetails.getProducerId());
        }
    }

    /**
     * Finds instance of export routine matching the aip query
     *
     * @param aipQueryId id of the aip query
     * @return export routine found
     */
    @Transactional
    public ExportRoutine findByAipQueryId(String aipQueryId) {
        return store.findByAipQueryId(aipQueryId);
    }

    /**
     * Deletes the instance of export routine from database.
     *
     * @param id id of the instance to delete
     */
    @Transactional
    public void delete(String id) {
        ExportRoutine entity = store.find(id);
        Utils.notNull(entity, () -> new MissingObject(store.getType(), id));

        store.delete(entity);

        log.debug("Canceling export routine job for export routine: " + id + ".");
        jobService.delete(entity.getJob());
    }


    @Autowired
    public void setStore(ExportRoutineStore store) {
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
    public void setAipQueryStore(AipQueryStore aipQueryStore) {
        this.aipQueryStore = aipQueryStore;
    }

    @Autowired
    public void setExportScript(@Value("${arclib.script.export}") Resource exportScript) {
        this.exportScript = exportScript;
    }
}
