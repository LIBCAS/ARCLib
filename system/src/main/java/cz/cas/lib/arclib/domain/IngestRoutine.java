package cz.cas.lib.arclib.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domainbase.domain.NamedObject;
import cz.cas.lib.core.scheduling.job.Job;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Zoznam balíkov, balíky sa priradzujú interne na service vrstve (nie pri '§'save' endpointe).
     */
    @JsonIgnore
    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "ingestRoutine")
    private List<Batch> currentlyProcessingBatches = new ArrayList<>();

    /**
     * Flag vďaka ktorému sa automaticky procesujú dávky pomocou prefixov.
     */
    private boolean auto;

}
