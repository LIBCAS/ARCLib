package cz.cas.lib.arclib.mail;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.security.authorization.assign.AssignedRoleService;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.mail.MailCenter;
import cz.cas.lib.core.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.mail.MessagingException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;

@Slf4j
@Service
public class ArclibMailCenter extends MailCenter {

    private AssignedRoleService assignedRoleService;

    /**
     * Send notification about the result of ingest.
     *
     * @param email   email address of the recipient
     * @param batchId id of the batch
     * @param result  result message of the ingest
     * @param created time when the notification was created
     */
    public void sendIngestResultNotification(String email, String batchId, String result, Instant created) {
        sendNotificationInternal(email, batchId, result, created, "templates/ingestResultNotification.vm");
        log.info("Sent notification mail with ingest result to address " + email + " the result is " + result + ".");
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
        sendNotificationInternal(email, aipId, result, created, "templates/aipDeletionAcknowledgedNotification.vm");
        log.info("Sent notification mail about acknowledgement of deletion of AIP with id " + aipId + " to address " +
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
        sendNotificationInternal(email, aipId, message, created, "templates/aipDeletionDisacknowledgedNotification.vm");
        log.info("Sent notification mail about disacknowledgement of deletion of AIP with id " + aipId + " to address " +
                email + ", information about the deletion request and user that disacknowledged the deletion: " + message + ".");
    }

    /**
     * Send notification about newly registered user.
     *
     * @param username of the registered user
     * @param created  time when the notification was created
     */
    public void sendNewUserRegisteredNotification(String username, Instant created) {
        Collection<User> recepients = assignedRoleService.getUsersWithRole(Roles.SUPER_ADMIN);
        recepients.forEach(user -> sendNotificationInternal(user.getEmail(), username, null, Instant.now(), "templates/newUserNotification.vm"));
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

            InputStream template = Utils.resource(templateName);
            transformAndSend(template, params, message);
        } catch (MessagingException | IOException ex) {
            throw new GeneralException(ex);
        }
    }

    @Inject
    public void setAssignedRoleService(AssignedRoleService assignedRoleService) {
        this.assignedRoleService = assignedRoleService;
    }
}
