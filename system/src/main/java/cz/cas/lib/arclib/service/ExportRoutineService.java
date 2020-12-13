package cz.cas.lib.arclib.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.domain.ExportRoutine;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.domainbase.exception.MissingAttribute;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.AipQueryStore;
import cz.cas.lib.arclib.store.ExportRoutineStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.index.dto.Result;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class ExportRoutineService {
    private Resource aipExportScript;

    private Resource xmlExportScript;

    private UserDetails userDetails;

    private String workspace;

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
        User creator = new User();
        creator.setId(userDetails.getId());
        exportRoutine.setCreator(creator);

        String exportLocationPath = null;
        try {
            switch (exportRoutine.getType()) {
                case XML_EXPORT:
                    exportLocationPath = scheduleXmlExportRoutineJob(exportRoutine);
                    break;
                case AIP_EXPORT_ALL_XMLS:
                    exportLocationPath = scheduleAipExportRoutineJob(exportRoutine, true);
                    break;
                case AIP_EXPORT_SINGLE_XML:
                    exportLocationPath = scheduleAipExportRoutineJob(exportRoutine, false);
            }
        } catch (JsonProcessingException e) {
            throw new GeneralException(e);
        }
        exportRoutine.setExportLocationPath(exportLocationPath);

        return store.save(exportRoutine);
    }

    /**
     * Creates and stores a scheduled job that performs the tasks to be executed by the export routine.
     * The job is performed by the GROOVY script specified in <code>aipExportScript</code>. The script is assigned
     * parameters according to the values derived from the provided instance of <code>exportRoutine</code>.
     *
     * @param exportRoutine
     * @param all           if <code>true</code> all AIP XMLs of all versions are exported,
     *                      if <code>false</code> only the most recent version of AIP XMLs are exported
     * @return export location path
     */
    @Transactional
    private String scheduleAipExportRoutineJob(ExportRoutine exportRoutine, boolean all) throws JsonProcessingException {
        log.debug("Scheduling aip export routine job for export routine: " + exportRoutine.getId());

        Job job = new Job();
        job.setActive(true);
        job.setTiming(Utils.toCron(exportRoutine.getExportTime()));
        job.setScriptType(ScriptType.GROOVY);
        try {
            String script = StreamUtils.copyToString(aipExportScript.getInputStream(), Charset.defaultCharset());
            job.setScript(script);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        AipQuery aipQuery = exportRoutine.getAipQuery();
        notNull(aipQuery, () -> new MissingAttribute(ExportRoutine.class, exportRoutine.getId(),
                "aipQuery"));

        aipQuery = aipQueryStore.find(aipQuery.getId());
        notNull(aipQuery, () -> new MissingObject(AipQuery.class, exportRoutine.getAipQuery().getId()));

        Result<IndexedArclibXmlDocument> result = aipQuery.getResult();
        List<IndexedArclibXmlDocument> items = result.getItems();
        List<String> aipIds = items.stream()
                .map(item -> item.getSipId())
                .collect(Collectors.toList());
        ObjectMapper mapper = new ObjectMapper();
        String jsonAipIds = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(aipIds);

        String uuid = UUID.randomUUID().toString();

        Path exportLocationPath = Paths.get(workspace, uuid);

        Map<String, String> jobParams = new HashMap<>();
        jobParams.put("aipIdsJson", jsonAipIds);
        jobParams.put("exportLocationPath", exportLocationPath.toString());
        jobParams.put("all", String.valueOf(all));
        jobParams.put("jobId", job.getId());
        job.setParams(jobParams);

        jobService.save(job);

        exportRoutine.setJob(job);
        store.save(exportRoutine);

        return exportLocationPath.toString();
    }

    /**
     * Creates and stores a scheduled job that performs the tasks to be executed by the export routine.
     * The job is performed by the GROOVY script specified in <code>xmlExportScript</code>. The script is assigned
     * parameters according to the values derived from the provided instance of <code>exportRoutine</code>.
     *
     * @param exportRoutine
     * @return export location path
     */
    @Transactional
    private String scheduleXmlExportRoutineJob(ExportRoutine exportRoutine) throws JsonProcessingException {
        log.debug("Scheduling XML export routine job for export routine: " + exportRoutine.getId());

        Job job = new Job();
        job.setActive(true);
        job.setTiming(Utils.toCron(exportRoutine.getExportTime()));
        job.setScriptType(ScriptType.GROOVY);
        try {
            String script = StreamUtils.copyToString(xmlExportScript.getInputStream(), Charset.defaultCharset());
            job.setScript(script);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        AipQuery aipQuery = exportRoutine.getAipQuery();
        notNull(aipQuery, () -> new MissingAttribute(ExportRoutine.class, exportRoutine.getId(),
                "aipQuery"));

        aipQuery = aipQueryStore.find(aipQuery.getId());
        notNull(aipQuery, () -> new MissingObject(AipQuery.class, exportRoutine.getAipQuery().getId()));

        Result<IndexedArclibXmlDocument> result = aipQuery.getResult();
        List<IndexedArclibXmlDocument> items = result.getItems();

        HashMap<String, List<Integer>> aipIdsAndVersions = new HashMap<>();
        for (IndexedArclibXmlDocument item : items) {
            String sipId = item.getSipId();
            Integer xmlVersionNumber = item.getXmlVersionNumber();

            List<Integer> versions = aipIdsAndVersions.get(sipId);
            if (versions == null) versions = new ArrayList<>();

            versions.add(xmlVersionNumber);
            aipIdsAndVersions.put(sipId, versions);
        }

        ObjectMapper mapper = new ObjectMapper();
        String aipIdsAndVersionsJson = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(aipIdsAndVersions);

        String uuid = UUID.randomUUID().toString();

        Path exportLocationPath = Paths.get(workspace, uuid);

        Map<String, String> jobParams = new HashMap<>();
        jobParams.put("aipIdsAndVersionsJson", aipIdsAndVersionsJson);
        jobParams.put("exportLocationPath", exportLocationPath.toString());
        jobParams.put("jobId", job.getId());
        job.setParams(jobParams);

        jobService.save(job);

        exportRoutine.setJob(job);
        store.save(exportRoutine);

        return exportLocationPath.toString();
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


    @Inject
    public void setStore(ExportRoutineStore store) {
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
    public void setAipQueryStore(AipQueryStore aipQueryStore) {
        this.aipQueryStore = aipQueryStore;
    }

    @Inject
    public void setWorkspace(@Value("${arclib.path.workspace}") String workspace) {
        this.workspace = workspace;
    }

    @Inject
    public void setAipExportScript(@Value("${arclib.script.exportRoutineAipExport}") Resource aipExportScript) {
        this.aipExportScript = aipExportScript;
    }

    @Inject
    public void setXmlExportScript(@Value("${arclib.script.exportRoutineXmlExport}") Resource xmlExportScript) {
        this.xmlExportScript = xmlExportScript;
    }
}
