package cz.cas.lib.core.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


import jakarta.mail.internet.MimeMessage;

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

    @Autowired
    public void setSender(JavaMailSender sender) {
        this.sender = sender;
    }
}
