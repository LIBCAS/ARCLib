package cz.cas.lib.core.mail;

import cz.cas.lib.core.service.Templater;
import freemarker.template.TemplateException;
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
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Central mailing component responsible for building up and sending mail messages from templates.
 * <p>
 * <p>
 * Will be further refactored.
 * </p>
 */
@ConditionalOnProperty(prefix = "mail", name = "excluded", havingValue = "false", matchIfMissing = true)
@Slf4j
@Component
public class MailCenter {

    private Boolean enabled;

    private AsyncMailSender sender;

    private String senderEmail;

    private String senderName;

    private String appLogo;

    protected String appName;

    private String appLink;

    private String appUrl;

    private Templater templater;

    protected MimeMessageHelper generalMessage(String emailTo, @Nullable String subject, boolean hasAttachment) throws MessagingException {
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

    protected Map<String, Object> generalArguments() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("appLogo", appLogo);
        arguments.put("appName", appName);
        arguments.put("appLink", appLink);
        arguments.put("senderEmail", senderEmail);
        return arguments;
    }

    protected void transformAndSend(String template, Map<String, Object> arguments, MimeMessageHelper helper)
            throws MessagingException, IOException, TemplateException {

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

    private String apUrl() {
        return appUrl;
    }

    @Inject
    public void setEnabled(@Value("${mail.enabled}") Boolean enabled) {
        this.enabled = enabled;
    }

    @Inject
    public void setSenderEmail(@Value("${spring.mail.username}") String senderEmail) {
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
    public void setSender(AsyncMailSender sender) {
        this.sender = sender;
    }

    @Inject
    public void setTemplater(Templater templater) {
        this.templater = templater;
    }
}
