package cz.cas.lib.arclib.domain;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
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
     * <p>
     * This config is merged {@link ProducerProfile#workflowConfig} and the result is used as a default for ingests and
     * stored to {@link #computedWorkflowConfig}
     */
    @Column(length = 10485760)
    private String workflowConfig;

    /**
     * Vypočtená konfigurace
     */
    @Column(length = 10485760)
    private String computedWorkflowConfig;

    /**
     * Cesta do prekladiska
     */
    @Column
    private String transferAreaPath;

    /**
     * Je aktívny debugovací režim
     */
    private boolean debuggingModeActive;

    /**
     * Je ingest workflow dávky deploynutý
     */
    private boolean bpmDefDeployed;

    /**
     * some ingest workflows resulted in incident which resolution is pending
     */
    private boolean pendingIncidents;

    /**
     * Validačný profil
     */
    @ManyToOne
    private ValidationProfile initialValidationProfile;

    /**
     * SIP profil
     */
    @ManyToOne
    private SipProfile initialSipProfile;

    /**
     * Definícia ingest workflow vo formáte BPMN
     */
    @ManyToOne
    private WorkflowDefinition initialWorkflowDefinition;
}
