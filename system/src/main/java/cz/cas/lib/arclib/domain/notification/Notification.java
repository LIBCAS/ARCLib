package cz.cas.lib.arclib.domain.notification;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.report.Report;
import cz.cas.lib.arclib.utils.NotificationComponentConverter;
import cz.cas.lib.core.scheduling.job.Job;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_notification")
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends DatedObject {

    @Enumerated(EnumType.STRING)
    @NonNull
    private NotificationType type;

    /**
     * Predmet e-mailu
     */
    @NotNull
    private String subject;

    /**
     * Obsah notifikácie
     */
    @NotNull
    private String message;

    /**
     * CRON
     */
    @NotNull
    private String cron;

    /**
     * ID entit, zavisi na {@link #type} -> {@link Format} / {@link Report}
     */
    @Convert(converter = NotificationComponentConverter.class) // Convert to JSON
    private List<NotificationElement> relatedEntities = new ArrayList<>();

    /**
     * JSON parametrizacia notifikacii.
     * Iba REPORT notifikacie mozu byt parametrizovane.
     * <p>
     * Priklad:
     * <pre>
     *   {
     *     "format": "PDF",
     *     "params": {
     *       "from": "1700-01-01T00:00:00Z"
     *     }
     *   }
     * </pre>
     */
    private String parameters;

    /**
     * Časový job.
     * Pouzite az na service vrstve.
     */
    @OneToOne
    private Job job;

    /**
     * Užívateľ, ktorý vytvoril plánovú notifikáciu
     */
    @ManyToOne
    private User creator;


    public List<String> obtainRelatedEntitiesIds() {
        return relatedEntities.stream()
                .map(NotificationElement::getId)
                .collect(Collectors.toList());
    }

    public enum NotificationType {
        FORMAT_REVISION,
        REPORT
    }
}
