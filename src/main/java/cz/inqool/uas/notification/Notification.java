package cz.inqool.uas.notification;

import cz.inqool.uas.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Nationalized;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Notification for a user
 *
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "uas_notification")
public class Notification extends DatedObject {
    /**
     * Subject
     */
    private String title;

    /**
     * Description of the event
     */
    private String description;

    /**
     * Id of User, who created this notification
     */
    protected String authorId;

    /**
     * Name of User, who created this notification
     */
    @Nationalized
    protected String authorName;

    /**
     * Id of User, for who this Notification is
     */
    protected String recipientId;

    /**
     * Name of User, for who this Notification is
     */
    @Nationalized
    private String recipientName;

    /**
     * Email of User, for who this Notification is
     */
    protected String recipientEmail;


    /**
     * One time notification
     */
    private Boolean flash;

    /**
     * Is notification already read
     */
    private Boolean read;

    /**
     * Should be sent also by email
     */
    private Boolean emailing;
}
