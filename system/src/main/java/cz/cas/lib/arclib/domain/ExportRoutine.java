package cz.cas.lib.arclib.domain;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import cz.cas.lib.core.scheduling.job.Job;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.time.Instant;

/**
 * Exportná rutina
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_export_routine")
@NoArgsConstructor
public class ExportRoutine extends DatedObject {
    /**
     * Vyhľadávací dotaz
     */
    @OneToOne
    private AipQuery aipQuery;

    /**
     * Čas plánovaného spustenia exportu
     */
    private Instant exportTime;

    /**
     * Cesta k exportnej lokácií
     */
    private String exportLocationPath;

    /**
     * Časový job
     */
    @OneToOne
    private Job job;

    /**
     * Užívateľ, ktorý vytvoril rutinu
     */
    @ManyToOne
    private User creator;

    /**
     * Typ exportnej rutiny
     */
    @Enumerated(value = EnumType.STRING)
    private ExportRoutineType type;
}
