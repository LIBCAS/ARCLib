package cz.cas.lib.arclib.domain;

import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.core.domain.NamedObject;
import cz.cas.lib.core.scheduling.job.Job;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;

/**
 * Importná rutina
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_ingest_routine")
@NoArgsConstructor
@AllArgsConstructor
public class IngestRoutine extends NamedObject {

    /**
     * Časový job
     */
    @OneToOne
    private Job job;

    /**
     * Profil dodávateľa
     */
    @ManyToOne
    private ProducerProfile producerProfile;

    /**
     * Cesta do prekladiska
     */
    private String transferAreaPath;

    /**
     * JSON konfigurácia ingest workflow
     */
    @Column(length = 10485760)
    private String workflowConfig;

    /**
     * Užívateľ, ktorý vytvoril rutinu
     */
    @ManyToOne
    private User creator;
}
