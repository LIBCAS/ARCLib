package cz.cas.lib.arclib.mail;

import cz.cas.lib.core.mail.AsyncMailSender;
import cz.cas.lib.core.service.Templater;
import freemarker.template.Configuration;
import helper.MockMailSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleStringCriteria;

import java.time.Instant;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class ArclibMailCenterTest {
    @InjectMocks
    protected MockMailSender mockMailSender = new MockMailSender();

    @InjectMocks
    protected AsyncMailSender sender = new AsyncMailSender();

    @InjectMocks
    protected ArclibMailCenter mailCenter = new ArclibMailCenter();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Configuration configuration = new Configuration();
        Templater templater = new Templater(configuration);

        sender.setSender(mockMailSender);
        mailCenter.setSender(sender);
        mailCenter.setTemplater(templater);
        mailCenter.setSenderEmail("noreply@test.cz");
        mailCenter.setSenderName("test");
        mailCenter.setAppName("arclib");
        mailCenter.setEnabled(true);
    }

    @Test
    public void sendIngestResultNotificationTest() {
        Criteria name = Criteria.where("name").expression(":*: -");
        mailCenter.sendIngestResultNotification("test@test.cz", "456", "Ingest has been successfully performed.",
                Instant.now());

        Object content = mockMailSender.getJavaMailProperties().get("mailContent");
        mockMailSender.getJavaMailProperties().remove("mailContent");
        System.out.println(content);
        assertThat(content, is(not(nullValue())));
        assertThat(content.toString(), containsString("Ingest has been successfully performed."));
    }
}

