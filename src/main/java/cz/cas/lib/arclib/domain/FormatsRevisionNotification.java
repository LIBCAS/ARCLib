package cz.cas.lib.arclib.domain;

import cz.cas.lib.core.domain.DatedObject;
import cz.cas.lib.core.scheduling.job.Job;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Plánovaná notifikácia ohľadne revízií formátových politík
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_formats_revision_notification")
@NoArgsConstructor
@AllArgsConstructor
public class FormatsRevisionNotification extends DatedObject {
    /**
     * Časový job
     */
    @OneToOne
    private Job job;

    /**
     * Obsah notifikácie
     */
    private String message;

    /**
     * CRON
     */
    private String cron;

    /**
     * Užívateľ, ktorý vytvoril plánovú notifikáciu
     */
    @ManyToOne
    private User creator;
}
