package helper;

import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class MockMailSender extends JavaMailSenderImpl {

    @Override
    public void send(final SimpleMailMessage message) throws MailException {
        try {

            final String content = message.getText();
            final Properties javaMailProperties = getJavaMailProperties();
            javaMailProperties.setProperty("mailContent", content);
        } catch (final Exception e) {
            throw new MailPreparationException(e);
        }
    }

    @Override
    public void send(final MimeMessage message) throws MailException {
        try {
            final String content = (String) message.getContent();
            final Properties javaMailProperties = getJavaMailProperties();
            javaMailProperties.setProperty("mailContent", content);
        } catch (final Exception e) {
            throw new MailPreparationException(e);
        }
    }
}
