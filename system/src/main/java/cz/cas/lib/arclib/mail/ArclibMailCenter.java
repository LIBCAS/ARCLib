package cz.cas.lib.arclib.mail;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.formatlibrary.service.FormatLibraryNotifier;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.authorization.role.UserRoleService;
import cz.cas.lib.arclib.service.UserService;
import cz.cas.lib.core.mail.MailCenter;
import cz.cas.lib.core.util.Utils;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ArclibMailCenter extends MailCenter implements FormatLibraryNotifier {

    private UserRoleService assignedRoleService;
    private UserService userService;

    /**
     * Send notification about the result of ingest.
     *
     * @param email   email address of the recipient
     * @param batchId id of the batch
     * @param result  result message of the ingest
     * @param created time when the notification was created
     */
    public void sendIngestResultNotification(String email, String batchId, String result, Instant created) {
        sendNotificationInternal(email, batchId, result, created, "templates/en/ingestResultNotification.ftl");
        log.debug("Sent notification mail with ingest result to address " + email + " the result is " + result + ".");
    }

    /**
     * Send notification on acknowledgement of deletion of an AIP package.
     *
     * @param email   email address of the recipient
     * @param aipId   id of the AIP package of which the deletion was acknowledged
     * @param result  result of the delete operation at archival storage
     * @param created time when the notification was created
     */
    public void sendAipDeletionAcknowledgedNotification(String email, String aipId, String result, Instant created) {
        sendNotificationInternal(email, aipId, result, created, "templates/en/aipDeletionAcknowledgedNotification.ftl");
        log.debug("Sent notification mail about acknowledgement of deletion of AIP with id " + aipId + " to address " +
                email + ", result of the operation at archival storage is " + result + ".");
    }

    /**
     * Send notification on disacknowledgement of deletion of an AIP package.
     *
     * @param email   email address of the recipient
     * @param aipId   id of the AIP package of which the deletion was disacknowledged
     * @param message information about the deletion request and user that disacknowledged the deletion
     * @param created time when the notification was created
     */
    public void sendAipDeletionDisacknowledgedNotification(String email, String aipId, String message, Instant created) {
        sendNotificationInternal(email, aipId, message, created, "templates/en/aipDeletionDisacknowledgedNotification.ftl");
        log.debug("Sent notification mail about disacknowledgement of deletion of AIP with id " + aipId + " to address " +
                email + ", information about the deletion request and user that disacknowledged the deletion: " + message + ".");
    }

    /**
     * Send notification about newly registered user.
     *
     * @param username of the registered user
     * @param created  time when the notification was created
     */
    public void sendNewUserRegisteredNotification(String username, Instant created) {
        Collection<User> recipients = assignedRoleService.getUsersWithPermission(Permissions.SUPER_ADMIN_PRIVILEGE);
        recipients.forEach(user -> sendNotificationInternal(user.getEmail(), username, null, created, "templates/en/newUserNotification.ftl"));
    }

    /**
     * Send notification about update of format library
     *
     * @param username username of the recipient
     * @param message  information about the formats being updated
     * @param created  time when the notification was created
     */
    public void sendFormatLibraryUpdateNotification(String username, String message, Instant created) {
        Collection<String> recipients = null;
        if (username != null) {
            User user = userService.findUserByUsername(username);
            if (user != null && user.getEmail() != null)
                recipients = Collections.singleton(user.getEmail());
            else {
                log.warn("Could not send notification mail about update of format library, email of user with username: " + username + " not found. Notification will be sent to all super admins.");
            }
        }
        if (recipients == null)
            recipients = assignedRoleService.getEmailsOfUsersWithPermission(Permissions.SUPER_ADMIN_PRIVILEGE);
        recipients.forEach(email -> sendNotificationInternal(email, null, message, created, "templates/en/formatLibraryUpdateNotification.ftl"));
        log.debug("Sent notification mail about update of format library to mail addresses " +
                Arrays.toString(recipients.toArray()) + ".");
    }

    /**
     * Send notification about necessary revision of format politics
     *
     * @param email   email address of the recipient
     * @param message information about the necessary format format politics revision
     * @param created time when the notification was created
     */
    public void sendFormatsRevisionNotification(String email, String message, Instant created) {
        sendNotificationInternal(email, null, message, created, "templates/en/formatsRevisionNotification.ftl");
        log.debug("Sent notification mail about necessary revision of format politics to " + email + ".");
    }


    /**
     * Send notification about necessary revision of format politics
     *
     * @param email         email address of the recipient
     * @param resultParam   information about the necessary format format politics revision
     * @param formatsOutput formatted strings with information about specific Formats
     * @param created       time when the notification was created
     */
    public void sendFormatsRevisionNotification(String email, String subject, String resultParam, List<String> formatsOutput, Instant created) {
        try {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd. MM. yyyy");

            MimeMessageHelper message = generalMessage(email, subject, false);

            Map<String, Object> params = generalArguments();
            params.put("result", resultParam);
            params.put("formats", formatsOutput);
            params.put("createdDate", Utils.extractDate(created).format(dateFormatter));
            params.put("createdTime", Utils.extractTime(created).format(timeFormatter));

            transformAndSend("templates/en/formatsRevisionNotification.ftl", params, message);
        } catch (MessagingException | IOException | TemplateException ex) {
            throw new GeneralException(ex);
        }
        log.debug("Sent notification mail about necessary revision of format politics to " + email + ".");
    }

    /**
     * Send notification with reports
     *
     * @param email       email address of the recipient
     * @param resultParam message about reports
     * @param reports     pairs of report name + file
     * @param created     time when the notification was created
     */
    public void sendReportNotification(String email, String subject, String resultParam, List<Pair<String, File>> reports, Instant created) {
        try {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd. MM. yyyy");

            MimeMessageHelper message = generalMessage(email, subject, true);
            for (Pair<String, File> reportNameAndFile : reports) {
                message.addAttachment(reportNameAndFile.getLeft(), reportNameAndFile.getRight());
            }

            Map<String, Object> params = generalArguments();
            params.put("result", resultParam);
            params.put("reports", reports.stream().map(Pair::getLeft).collect(Collectors.toList()));
            params.put("createdDate", Utils.extractDate(created).format(dateFormatter));
            params.put("createdTime", Utils.extractTime(created).format(timeFormatter));

            transformAndSend("templates/en/reportNotification.ftl", params, message);
        } catch (MessagingException | IOException | TemplateException ex) {
            throw new GeneralException(ex);
        }
        log.debug("Sent notification mail with reports to " + email + ".");
    }

    /**
     * Send notification that some transfer is not reachable
     *
     * @param email   email address of the recipient
     * @param message information about the transfer area tha is not reachable
     * @param created time when the notification was created
     */
    public void sendTransferAreaNotReachableNotification(String email, String message, Instant created) {
        sendNotificationInternal(email, null, message, created, "templates/en/transferAreaNotReachableNotification.ftl");
        log.debug("Sent transfer area not reachable notification to mail " +
                email + ".");
    }

    /**
     * Send notification about unsupported PRONOM value
     *
     * @param username username of the recipient
     * @param message  information about the formats being updated
     * @param created  time when the notification was created
     */
    public void sendUnsupportedPronomValueNotification(String username, String message, Instant created) {
        if (username != null) {
            User user = userService.findUserByUsername(username);
            if (user != null && user.getEmail() != null) {
                sendNotificationInternal(user.getEmail(), null, message, created, "templates/en/unsupportedPronomValueNotification.ftl");
                log.debug("Sent notification mail about unsupported PRONOM value to mail address " +
                        user.getEmail() + ", message: " + message + ".");
                return;
            }
        }
        log.warn("Could not send notification mail about unsupported PRONOM, email of user with username: " + username + " not found.");
    }

    /**
     * Send notification about detection of the new tool version and creation of entity for it
     *
     * @param oldVersion of the tool, may be null
     * @param newVersion of the tool
     */
    public void sendNewToolVersionNotification(Tool oldVersion, Tool newVersion) {
        Collection<User> recipients = assignedRoleService.getUsersWithPermission(Permissions.SUPER_ADMIN_PRIVILEGE);
        String msg = "Name: " + newVersion.getName() + "\nPrevious version: " + (oldVersion == null ? "no previous version" : oldVersion.getVersion()) + "\nNew version: " + newVersion.getVersion();
        recipients.forEach(user -> sendNotificationInternal(user.getEmail(), newVersion.getId(), msg, Instant.now(), "templates/en/newToolVersionNotification.ftl"));
        log.debug("Sent notification mail about new tool version detected and created to mail addresses " +
                Arrays.toString(recipients.stream().map(User::getEmail).toArray()) + ", message: " + msg + ".");
    }

    public void sendReingestJobFailedNotification(Exception ex) {
        String msg = ExceptionUtils.getStackTrace(ex);
        Collection<User> recipients = assignedRoleService.getUsersWithPermission(Permissions.SUPER_ADMIN_PRIVILEGE);
        recipients.forEach(user -> sendNotificationInternal(user.getEmail(), null, msg, Instant.now(), "templates/en/reingestJobFailedNotification.ftl"));
        log.debug("Sent notification mail about reingest job run failure to mail addresses " +
                Arrays.toString(recipients.stream().map(User::getEmail).toArray()) + ", message: " + msg + ".");
    }

    private void sendNotificationInternal(String email, String externalId, String result, Instant created, String templateName) {
        try {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd. MM. yyyy");

            MimeMessageHelper message = generalMessage(email, appName, false);

            Map<String, Object> params = generalArguments();
            params.put("externalId", externalId);
            params.put("result", result);
            params.put("createdDate", Utils.extractDate(created).format(dateFormatter));
            params.put("createdTime", Utils.extractTime(created).format(timeFormatter));

            transformAndSend(templateName, params, message);
        } catch (MessagingException | IOException | TemplateException ex) {
            throw new GeneralException(ex);
        }
    }

    @Autowired
    public void setAssignedRoleService(UserRoleService assignedRoleService) {
        this.assignedRoleService = assignedRoleService;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
