package cz.inqool.uas.mail;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.mail.internet.MimeMessage;

/**
 * Asynchronous wrapper around Spring {@link JavaMailSender}.
 */
@Component
public class AsyncMailSender {
    private JavaMailSender sender;

    @Async
    public void send(MimeMessage msg) {
        sender.send(msg);
    }

    public MimeMessage create() {
        return sender.createMimeMessage();
    }

    @Inject
    public void setSender(JavaMailSender sender) {
        this.sender = sender;
    }
}
