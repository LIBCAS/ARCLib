package cz.cas.lib.arclib.domain;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.core.domain.DatedObject;
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
 * Dávka
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "arclib_batch")
public class Batch extends DatedObject {

    /**
     * Zoznam SIP balíkov
     */
    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "batch")
    private List<IngestWorkflow> ingestWorkflows = new ArrayList<>();

    /**
     * Stav spracovania
     */
    @Enumerated(EnumType.STRING)
    private BatchState state;

    /**
     * Profil dodávateľa
     */
    @ManyToOne
    private ProducerProfile producerProfile;

    /**
     * Konfigurácia ingest workflow vo formáte JSON
     */
    @Column(length = 10485760)
    private String workflowConfig;

    /**
     * Cesta do prekladiska
     */
    @Column
    private String transferAreaPath;

    /**
     * Je aktívny debugovací režim
     */
    private boolean debuggingModeActive;
}
