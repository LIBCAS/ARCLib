package cz.cas.lib.arclib.domain.export;

import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import cz.cas.lib.core.scheduling.job.Job;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
    @NotNull
    private AipQuery aipQuery;

    /**
     * Čas plánovaného spustenia exportu
     */
    @NotNull
    private Instant exportTime;

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

    @Embedded
    @NotNull
    @Valid
    private ExportConfig config;
}
