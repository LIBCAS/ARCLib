package cz.cas.lib.arclib.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domain.notification.Notification;
import cz.cas.lib.arclib.domain.notification.NotificationElement;
import cz.cas.lib.arclib.domain.notification.ReportParameters;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.formatlibrary.service.FormatService;
import cz.cas.lib.arclib.index.autocomplete.AutoCompleteItem;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.report.ExportFormat;
import cz.cas.lib.arclib.report.ExporterService;
import cz.cas.lib.arclib.report.Report;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.authorization.role.UserRoleService;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.IndexedFormatStore;
import cz.cas.lib.arclib.store.NotificationStore;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.scheduling.job.Job;
import cz.cas.lib.core.scheduling.job.JobService;
import cz.cas.lib.core.script.ScriptType;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class NotificationService {

    private NotificationStore store;
    private JobService jobService;
    private UserDetails userDetails;
    private UserRoleService assignedRoleService;
    private ArclibMailCenter arclibMailCenter;
    private FormatService formatService;
    private IndexedFormatStore indexedFormatStore;
    private ReportService reportService;
    private ExporterService exporterService;
    private Resource formatsRevisionNotificationScript;
    private Resource reportNotificationScript;
    private ObjectMapper objectMapper;

    /**
     * Saves the instance of formats revision notification to database. Furthermore it creates a job that performs the
     * scheduled tasks for the formats revision notification.
     *
     * @param notification formats revision notification to be saved
     * @return saved formats revision notification
     */
    @Transactional
    public Notification save(Notification notification) {
        User creator = new User();
        creator.setId(userDetails.getId());
        notification.setCreator(creator);

        Notification existingNotification = store.find(notification.getId());
        if (existingNotification != null) {
            eq(existingNotification.getType(), notification.getType(), () -> new ForbiddenException("Change of notification type is not allowed."));
            removeScheduledJob(existingNotification);
        }

        store.identifyRelatedEntities(notification);
        switch (notification.getType()) {
            case FORMAT_REVISION:
                log.debug("Scheduling format revision notification.");
                notification.setParameters(null); // FORMAT_REVISION notifications cannot be parametrized
                createScheduledJob(notification, "Format revision notification sender.", formatsRevisionNotificationScript);
                break;
            case REPORT:
                log.debug("Scheduling report notification.");
                validateReportNotificationParameters(notification);
                createScheduledJob(notification, "Report notification sender.", reportNotificationScript);
                break;
        }
        return store.save(notification);
    }

    /**
     * Gets instance of formats revision notification by id.
     *
     * @param id id of the instance
     * @return the retrieved instance
     */
    @Transactional
    public Notification find(String id) {
        Notification notification = store.find(id);
        notNull(notification, () -> new MissingObject(Notification.class, id));
        return notification;
    }

    /**
     * Deletes the instance of formats revision notification from database.
     *
     * If notification is of REPORT type, then all related exported report files are deleted from file system.
     *
     * @param id id of the instance to delete
     */
    @Transactional
    public void delete(String id) {
        Notification entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        // remove associated exported reports from filesystem
        if (entity.getType() == Notification.NotificationType.REPORT) {
            List<NotificationElement> relatedEntities = entity.getRelatedEntities();
            List<Report> reports = relatedEntities.stream()
                    .map(element -> {
                        Report report = new Report();
                        report.setId(element.getId());
                        return report;
                    })
                    .collect(Collectors.toList());

            log.info(String.format("Deleting exported report files (count: %d) for notification: %s...", reports.size(), id));

            ReportParameters reportParameters;
            try {
                reportParameters = objectMapper.readValue(entity.getParameters(), ReportParameters.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            reports.forEach(report -> exporterService.deleteExportedReportFile(report, reportParameters.getFormat()));
        }

        store.delete(entity);
        removeScheduledJob(entity);
    }

    /**
     * Creates and stores a scheduled job that performs the notification services for planed format politics revisions.
     * The job is performed by the GROOVY script specified in <code>formatsRevisionNotificationScript</code>.
     * The script is assigned parameters according to the values derived from the provided instance of
     * <code>FormatsRevisionNotification</code>.
     *
     * @implNote Call from {@code @Transactional} method
     */
    private void createScheduledJob(Notification notification, String jobName, Resource reportNotificationScript) {
        Job job = new Job();
        job.setTiming(notification.getCron());
        job.setName(jobName);
        job.setParams(Map.of(
                "message", notification.getMessage(),
                "notificationId", notification.getId()
        ));
        job.setScriptType(ScriptType.GROOVY);
        try {
            String script = StreamUtils.copyToString(reportNotificationScript.getInputStream(), Charset.defaultCharset());
            job.setScript(script);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        job.setActive(true);
        jobService.save(job);

        notification.setJob(job);
    }

    private void removeScheduledJob(Notification notification) {
        if (notification.getJob() != null) {
            log.debug(String.format("Removing scheduled job for %s...", notification));
            jobService.delete(notification.getJob());
            notification.setJob(null);
        }
    }

    private void validateReportNotificationParameters(Notification notification) {
        eq(notification.getType(), Notification.NotificationType.REPORT, () -> new BadArgument("Notification must be of REPORT type to validate its parameters"));
        notNull(notification.getParameters(), () -> new BadArgument("Report Notification parameters cannot be null."));
        ReportParameters reportParameters;
        try {
            reportParameters = objectMapper.readValue(notification.getParameters(), ReportParameters.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ExportFormat format = reportParameters.getFormat();
        notNull(format, () -> new BadArgument("Report Notification parameters' format cannot be null."));
    }

    /**
     * Sends email notification about the necessary revisions of format politics to all users with permissions:
     * <ul>
     *   <li><code>Permissions.SUPER_ADMIN_PRIVILEGE</code></li>
     *   <li><code>Permissions.ADMIN_PRIVILEGE</code></li>
     * </ul>
     */
    public void sendFormatsRevisionNotification(String notificationId) {
        log.debug("Preparing to send FORMAT REVISION notifications...");

        Notification notification = this.find(notificationId);
        List<Format> formats = formatService.findAllInList(notification.obtainRelatedEntitiesIds());

        List<String> formatsOutput = formats.stream()
                .map(format -> String.format("[%d] %s [PUID: %s]", format.getFormatId(), format.getFormatName(), format.getPuid()))
                .collect(Collectors.toList());

        Collection<User> toBeNotified = new HashSet<>();
        toBeNotified.addAll(assignedRoleService.getUsersWithPermission(Permissions.SUPER_ADMIN_PRIVILEGE));
        toBeNotified.addAll(assignedRoleService.getUsersWithPermission(Permissions.ADMIN_PRIVILEGE));
        toBeNotified.forEach(user -> arclibMailCenter.sendFormatsRevisionNotification(user.getEmail(), notification.getSubject(), notification.getMessage(), formatsOutput, Instant.now()));
    }

    /**
     * Sends email notification with associated exported reports to all users with permissions:
     * <ul>
     *   <li><code>Permissions.SUPER_ADMIN_PRIVILEGE</code></li>
     *   <li><code>Permissions.ADMIN_PRIVILEGE</code></li>
     * </ul>
     */
    public void sendReportNotification(String notificationId) {
        log.debug("Preparing to send REPORT notifications...");

        Notification notification = this.find(notificationId);

        ReportParameters reportParameters;
        try {
            reportParameters = objectMapper.readValue(notification.getParameters(), ReportParameters.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        List<Report> reports = reportService.findAllInList(notification.obtainRelatedEntitiesIds());
        reports.forEach(report -> exporterService.exportToFileSystem(report, reportParameters.getFormat(), reportParameters.getParams()));

        List<Pair<String, File>> reportNamesAndFiles = reports.stream()
                .map(report -> Pair.of(report.getName(), exporterService.getExportedReportFile(report, reportParameters.getFormat())))
                .collect(Collectors.toList());

        Collection<User> toBeNotified = new HashSet<>();
        toBeNotified.addAll(assignedRoleService.getUsersWithPermission(Permissions.SUPER_ADMIN_PRIVILEGE));
        toBeNotified.addAll(assignedRoleService.getUsersWithPermission(Permissions.ADMIN_PRIVILEGE));
        toBeNotified.forEach(user -> arclibMailCenter.sendReportNotification(user.getEmail(), notification.getSubject(), notification.getMessage(), reportNamesAndFiles, Instant.now()));
    }

    @Transactional
    public Collection<Notification> findAll() {
        return store.findAll();
    }

    @Transactional
    public Result<AutoCompleteItem> listAutoCompleteReports(Params params) {
        return reportService.listAutoComplete(params);
    }

    @Transactional
    public Result<AutoCompleteItem> listAutoCompleteFormats(Params params) {
        // using direct access through indexed store instead of FormatService
        // because 'formatLibrary' module cannot access Result and Params classes
        return indexedFormatStore.listAutoComplete(params);
    }

    @Inject
    public void setStore(NotificationStore store) {
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
    public void setAssignedRoleService(UserRoleService assignedRoleService) {
        this.assignedRoleService = assignedRoleService;
    }

    @Inject
    public void setFormatService(FormatService formatService) {
        this.formatService = formatService;
    }

    @Inject
    public void setReportService(ReportService reportService) {
        this.reportService = reportService;
    }

    @Inject
    public void setExporterService(ExporterService exporterService) {
        this.exporterService = exporterService;
    }

    @Inject
    public void setIndexedFormatStore(IndexedFormatStore indexedFormatStore) {
        this.indexedFormatStore = indexedFormatStore;
    }

    @Inject
    public void setFormatsRevisionNotificationScript(@Value("${arclib.script.formatsRevisionNotification}") Resource formatsRevisionNotificationScript) {
        this.formatsRevisionNotificationScript = formatsRevisionNotificationScript;
    }

    @Inject
    public void setReportNotificationScript(@Value("${arclib.script.reportNotification}") Resource reportNotificationScript) {
        this.reportNotificationScript = reportNotificationScript;
    }

    @Inject
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
