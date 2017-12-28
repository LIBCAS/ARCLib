package cz.cas.lib.arclib.mail;

import cz.inqool.uas.exception.GeneralException;
import cz.inqool.uas.mail.MailCenter;
import cz.inqool.uas.util.Utils;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ArclibMailCenter extends MailCenter {
    /**
     * Send notification about the result of ingest.
     *
     * @param email   email address of the recipient
     * @param sipId   id of the SIP package that has been processed
     * @param result  result message of the ingest
     * @param created time when the notification was created
     */
    public void sendIngestResultNotification(String email, String sipId, String result, Instant created) {
        sendNotificationInternal(email, sipId, result, created, "templates/ingestResultNotification.vm");
    }

    /**
     * Send notification on saving of an AIP package.
     *
     * @param email   email address of the recipient
     * @param sipId   id of the SIP package that has been processed
     * @param result  result of the save operation
     * @param created time when the notification was created
     */
    public void sendAipSavedNotification(String email, String sipId, String result, Instant created) {
        sendNotificationInternal(email, sipId, result, created, "templates/aipSavedNotification.vm");
    }

    private void sendNotificationInternal(String email, String sipId, String result, Instant created, String templateName) {
        try {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd. MM. yyyy");

            MimeMessageHelper message = generalMessage(email, appName, false);

            Map<String, Object> params = generalArguments();
            params.put("sipId", sipId);
            params.put("result", result);
            params.put("createdDate", Utils.extractDate(created).format(dateFormatter));
            params.put("createdTime", Utils.extractTime(created).format(timeFormatter));

            InputStream template = Utils.resource(templateName);
            transformAndSend(template, params, message);
        } catch (MessagingException | IOException ex) {
            throw new GeneralException(ex);
        }
    }
}
