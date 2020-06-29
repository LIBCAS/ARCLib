package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.FormatsRevisionNotification;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.security.authorization.assign.AssignedRoleService;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.FormatsRevisionNotificationStore;
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
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;

import static cz.cas.lib.core.util.Utils.asMap;
import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class FormatsRevisionNotificationService {
    private UserDetails userDetails;
    private FormatsRevisionNotificationStore store;
    @Getter
    private JobService jobService;
    private Resource formatsRevisionNotificationScript;
    private AssignedRoleService assignedRoleService;
    private ArclibMailCenter arclibMailCenter;

    /**
     * Saves the instance of formats revision notification to database. Furthermore it creates a job that performs the
     * scheduled tasks for the formats revision notification.
     *
     * @param formatsRevisionNotification formats revision notification to be saved
     * @return saved formats revision notification
     */
    @Transactional
    public FormatsRevisionNotification save(FormatsRevisionNotification formatsRevisionNotification) {
        User creator = new User();
        creator.setId(userDetails.getId());
        formatsRevisionNotification.setCreator(creator);

        scheduleFormatsRevisionNotificationJob(formatsRevisionNotification);

        return store.save(formatsRevisionNotification);
    }

    /**
     * Creates and stores a scheduled job that performs the notification services for planed format politics revisions.
     * The job is performed by the GROOVY script specified in <code>formatsRevisionNotificationScript</code>. The script is assigned
     * parameters according to the values derived from the provided instance of <code>FormatsRevisionNotification</code>.
     *
     * @param formatsRevisionNotification
     * @return export location path
     */
    @Transactional
    private void scheduleFormatsRevisionNotificationJob(FormatsRevisionNotification formatsRevisionNotification) {
        log.debug("Scheduling format revision notifications.");

        Job job = new Job();
        job.setTiming(formatsRevisionNotification.getCron());
        job.setName("Format revision notification sender.");
        job.setParams(asMap("message", formatsRevisionNotification.getMessage()));
        job.setScriptType(ScriptType.GROOVY);
        try {
            String script = StreamUtils.copyToString(formatsRevisionNotificationScript.getInputStream(), Charset.defaultCharset());
            job.setScript(script);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        job.setActive(true);
        jobService.save(job);

        formatsRevisionNotification.setJob(job);
        store.save(formatsRevisionNotification);
    }

    /**
     * Gets instance of formats revision notification by id.
     *
     * @param id id of the instance
     * @return the retrieved instance
     */
    @Transactional
    public FormatsRevisionNotification find(String id) {
        FormatsRevisionNotification entity = store.find(id);
        notNull(entity, () -> new MissingObject(FormatsRevisionNotification.class, id));
        return entity;
    }

    /**
     * Deletes the instance of formats revision notification from database.
     *
     * @param id id of the instance to delete
     */
    @Transactional
    public void delete(String id) {
        FormatsRevisionNotification entity = store.find(id);
        Utils.notNull(entity, () -> new MissingObject(store.getType(), id));

        store.delete(entity);

        log.debug("Canceling formats revision notification job for formats revision notification: " + id + ".");
        jobService.delete(entity.getJob());
    }

    /**
     * Sends email notification about the necessary revisions of format politics to all users with roles <code>Roles.ADMIN</code>
     * and <code>Roles.SUPER_ADMIN</code>.
     *
     * @param message body of the notification
     */
    public void sendFormatsRevisionNotification(String message) {
        Collection<User> toBeNotified = new HashSet<>();
        toBeNotified.addAll(assignedRoleService.getUsersWithRole(Roles.SUPER_ADMIN));
        toBeNotified.addAll(assignedRoleService.getUsersWithRole(Roles.ADMIN));
        toBeNotified.stream()
                .forEach(user -> arclibMailCenter.sendFormatsRevisionNotification(user.getEmail(), message, Instant.now()));
    }

    public Collection<FormatsRevisionNotification> findAll() {
        return store.findAll();
    }

    @Inject
    public void setStore(FormatsRevisionNotificationStore store) {
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
    public void setArclibMailCenter(ArclibMailCenter arclibMailCenter) {
        this.arclibMailCenter = arclibMailCenter;
    }

    @Inject
    public void setAssignedRoleService(AssignedRoleService assignedRoleService) {
        this.assignedRoleService = assignedRoleService;
    }

    @Inject
    public void setFormatsRevisionNotificationScript(@Value("${arclib.script.formatsRevisionNotification}")
                                                             Resource formatsRevisionNotificationScript) {
        this.formatsRevisionNotificationScript = formatsRevisionNotificationScript;
    }
}
