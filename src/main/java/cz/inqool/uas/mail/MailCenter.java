package cz.inqool.uas.mail;

import cz.inqool.uas.exception.GeneralException;
import cz.inqool.uas.service.Templater;
import cz.inqool.uas.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static cz.inqool.uas.util.Utils.extractDate;
import static cz.inqool.uas.util.Utils.extractTime;

/**
 * Central mailing component responsible for building up and sending mail messages from templates.
 *
 * <p>
 *     Will be further refactored.
 * </p>
 */
@ConditionalOnProperty(prefix = "mail", name = "excluded", havingValue = "false", matchIfMissing = true)
@Slf4j
@Component
public class MailCenter {

    private Boolean enabled;

    private AsyncMailSender sender;

    private Templater templater;

    private String senderEmail;

    private String senderName;

    private String appLogo;

    private String appName;

    private String appLink;

    private String appUrl;

    private String pathTasks;

    public void sendAssignment(String email, String businessKey, String taskId, String taskName, String taskKey, Instant created, Instant dueDate) {
        sendAssignmentInternal(email, businessKey, taskId, taskName, taskKey, created, dueDate, null,"templates/taskNotification.vm");
    }

    public void sendPoolAssignment(String email, String businessKey, String taskId, String taskName, String taskKey, Instant created, Instant dueDate) {
        sendAssignmentInternal(email, businessKey, taskId, taskName, taskKey, created, dueDate, null,"templates/taskPoolNotification.vm");
    }

    public void sendPoolAssignmentOther(String email, String businessKey, String taskId, String taskName, String taskKey, Instant created, Instant dueDate, String otherName) {
        sendAssignmentInternal(email, businessKey, taskId, taskName, taskKey, created, dueDate, otherName,"templates/taskPoolNotificationAssigned.vm");
    }

    private MimeMessageHelper generalMessage(String emailTo, @Nullable String subject, boolean hasAttachment) throws MessagingException {
        MimeMessage message = sender.create();

        // use the true flag to indicate you need a multipart message
        MimeMessageHelper helper = new MimeMessageHelper(message, hasAttachment);

        if (emailTo != null) {
            helper.setTo(emailTo);
        }

        if (subject != null) {
            helper.setSubject(subject);
        }

        try {
            helper.setFrom(senderEmail, senderName);
        } catch (UnsupportedEncodingException ex) {
            log.warn("Can not set email 'from' encoding, fallbacking.");
            helper.setFrom(senderEmail);
        }

        return helper;
    }

    private Map<String, Object> generalArguments() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("appLogo", appLogo);
        arguments.put("appName", appName);
        arguments.put("appLink", appLink);
        arguments.put("senderEmail", senderEmail);
        return arguments;
    }

    private void transformAndSend(InputStream template, Map<String, Object> arguments, MimeMessageHelper helper)
            throws MessagingException, IOException {

        if (!enabled) {
            log.warn("Mail message was silently consumed because mail system is disabled.");
            return;
        }

        String text = templater.transform(template, arguments);
        helper.setText(text, true);

        MimeMessage message = helper.getMimeMessage();

        if (message.getAllRecipients() != null && message.getAllRecipients().length > 0) {
            sender.send(message);
        } else {
            log.warn("Mail message was silently consumed because there were no recipients.");
        }
    }

    public void sendNotification(String email, String title, String description) {
        try {
            MimeMessageHelper message = generalMessage(email, appName + ": " + title, false);

            Map<String, Object> params = generalArguments();
            params.put("content", description);

            InputStream template = Utils.resource("templates/notification.vm");
            transformAndSend(template, params, message);

        } catch (MessagingException | IOException ex) {
            throw new GeneralException(ex);
        }
    }

    private void sendAssignmentInternal(String email, String businessKey, String taskId, String taskName,
                                        String taskKey, Instant created, Instant dueDate, String otherName,
                                        String templateName) {
        try {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd. MM. yyyy");

            MimeMessageHelper message = generalMessage(email, appName + " " + businessKey + ": " + taskName, false);

            Map<String, Object> params = generalArguments();
            params.put("taskName", taskName);
            params.put("otherName", otherName);
            params.put("createdDate", extractDate(created).format(dateFormatter));
            params.put("createdTime", extractTime(created).format(timeFormatter));

            if (dueDate != null) {
                params.put("dueDate", extractDate(dueDate).format(dateFormatter));
                params.put("dueTime", extractTime(dueDate).format(timeFormatter));
            }

            params.put("serverUrl", taskUrl(taskId, taskKey));

            InputStream template = Utils.resource(templateName);
            transformAndSend(template, params, message);
        } catch (MessagingException | IOException ex) {
            throw new GeneralException(ex);
        }
    }

    private String apUrl() {
        return appUrl;
    }

    private String taskUrl(String id, String taskKey) {
        return apUrl() + pathTasks.replace("{id}", id).replace("{key}", taskKey);
    }

    @Inject
    public void setEnabled(@Value("${mail.enabled:false}") Boolean enabled) {
        this.enabled = enabled;
    }

    @Inject
    public void setSenderEmail(@Value("${mail.sender.email}") String senderEmail) {
        this.senderEmail = senderEmail;
    }

    @Inject
    public void setSenderName(@Value("${mail.sender.name}") String senderName) {
        this.senderName = senderName;
    }

    @Inject
    public void setAppLogo(@Value("${mail.app.logo}") String appLogo) {
        this.appLogo = appLogo;
    }

    @Inject
    public void setAppName(@Value("${mail.app.name}") String appName) {
        this.appName = appName;
    }

    @Inject
    public void setAppLink(@Value("${mail.app.link}") String appLink) {
        this.appLink = appLink;
    }

    @Inject
    public void setAppUrl(@Value("${mail.app.url}") String appUrl) {
        this.appUrl = appUrl;
    }

    @Inject
    public void setPathTasks(@Value("${mail.app.path.tasks}") String pathTasks) {
        this.pathTasks = pathTasks;
    }

    @Inject
    public void setSender(AsyncMailSender sender) {
        this.sender = sender;
    }

    @Inject
    public void setTemplater(Templater templater) {
        this.templater = templater;
    }
}
