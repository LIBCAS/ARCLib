package cz.inqool.uas.bpm.config.notify;

import cz.inqool.uas.exception.MissingAttribute;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.mail.MailCenter;
import cz.inqool.uas.security.UserDetails;
import cz.inqool.uas.security.UserDetailsService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.task.IdentityLink;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Objects;
import java.util.Set;

import static cz.inqool.uas.util.Utils.notNull;

/**
 * Holds all the notification methods.
 */
@ConditionalOnProperty(prefix = "mail", name = "excluded", havingValue = "false", matchIfMissing = true)
@Service
public class NotificationCenter {
    private UserDetailsService userDetailsService;

    private MailCenter mailCenter;

    @Inject
    public NotificationCenter(UserDetailsService userDetailsService, MailCenter mailCenter) {
        this.userDetailsService = userDetailsService;
        this.mailCenter = mailCenter;
    }

    /**
     * Notifies about assigned task.
     *
     * @param task task no notify about
     */
    public void notifyAssign(DelegateTask task) {
        String assignee = task.getAssignee();
        notNull(assignee, () -> new MissingAttribute(task, "assignee"));

        UserDetails user = userDetailsService.loadUserById(assignee);
        notNull(user, () -> new MissingObject(UserDetails.class, assignee));

        if (hasTaskNotifyEnabled(task)) {
            mailCenter.sendAssignment(user.getEmail(), task.getExecution().getBusinessKey(),
                    task.getId(), task.getName(), task.getTaskDefinitionKey(),
                    task.getCreateTime().toInstant(),
                    task.getDueDate() != null ? task.getDueDate().toInstant() : null);
        }

        // send pool assigned mail to other candidates
        Set<IdentityLink> links = task.getCandidates();
        links.stream()
                .filter(link -> link.getUserId() != null)
                .map(IdentityLink::getUserId)
                .filter(username -> !assignee.equals(username))
                .forEach(username -> {
                    UserDetails otherUser = userDetailsService.loadUserById(username);
                    notNull(otherUser, () -> new MissingObject(UserDetails.class, username));

                    if (hasTaskNotifyEnabled(task)) {
                        mailCenter.sendPoolAssignmentOther(otherUser.getEmail(), task.getExecution().getBusinessKey(),
                                task.getId(), task.getName(), task.getTaskDefinitionKey(),
                                task.getCreateTime().toInstant(),
                                task.getDueDate() != null ? task.getDueDate().toInstant() : null,
                                user.getFullName());
                    }
                });
    }

    /**
     * Notifies about unassigned (pooled) task.
     *
     * @param task task no notify about
     */
    public void notifyPool(DelegateTask task) {
        Set<IdentityLink> links = task.getCandidates();

        links.stream()
                .filter(link -> link.getUserId() != null)
                .map(IdentityLink::getUserId)
                .forEach(username -> {
                    UserDetails user = userDetailsService.loadUserById(username);
                    notNull(user, () -> new MissingObject(UserDetails.class, username));

                    if (hasTaskNotifyEnabled(task)) {
                        mailCenter.sendPoolAssignment(user.getEmail(), task.getExecution().getBusinessKey(),
                                task.getId(), task.getName(), task.getTaskDefinitionKey(),
                                task.getCreateTime().toInstant(),
                                task.getDueDate() != null ? task.getDueDate().toInstant() : null);
                    }
                });
    }

    /**
     * Notifies about assigned task due date imminent.
     *
     * @param task task no notify about
     */
    public void notifyDueDate(DelegateTask task) {
        String assignee = task.getAssignee();
        notNull(assignee, () -> new MissingAttribute(task, "assignee"));

        UserDetails user = userDetailsService.loadUserById(assignee);
        notNull(user, () -> new MissingObject(UserDetails.class, assignee));

        if (hasTaskNotifyEnabled(task)) {
            mailCenter.sendAssignment(user.getEmail(), task.getExecution().getBusinessKey(),
                    task.getId(), task.getName(), task.getTaskDefinitionKey(),
                    task.getCreateTime().toInstant(),
                    task.getDueDate() != null ? task.getDueDate().toInstant() : null);
        }
    }

    /**
     * Notifies about unassigned (pooled) task due date imminent.
     *
     * @param task task no notify about
     */
    public void notifyPoolDueDate(DelegateTask task) {
        Set<IdentityLink> links = task.getCandidates();

        links.stream()
                .filter(link -> link.getUserId() != null)
                .map(IdentityLink::getUserId)
                .forEach(username -> {
                    UserDetails user = userDetailsService.loadUserById(username);
                    notNull(user, () -> new MissingObject(UserDetails.class, username));

                    if (hasTaskNotifyEnabled(task)) {
                        mailCenter.sendPoolAssignment(user.getEmail(), task.getExecution().getBusinessKey(),
                                task.getId(), task.getName(), task.getTaskDefinitionKey(),
                                task.getCreateTime().toInstant(),
                                task.getDueDate() != null ? task.getDueDate().toInstant() : null);
                    }
                });
    }

    private boolean hasTaskNotifyEnabled(DelegateTask task) {
        Object notification = task.getVariable("sendNotification");

        return notification == null || Objects.equals(notification, "true");
    }
}
