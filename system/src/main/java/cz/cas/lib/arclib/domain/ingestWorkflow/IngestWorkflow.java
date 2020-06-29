package cz.cas.lib.arclib.domain.ingestWorkflow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.domain.VersioningLevel;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.time.Instant;

/**
 * This entity represents one attempt to store new AIP XML. Its created during:
 * <ul>
 *     <li>ingest of a completely new SIP (original authorialId) through ingest batch</li>
 *     <li>data update of existing SIP through ingest batch</li>
 *     <li>metadata update of existing SIP through ingest batch</li>
 *     <li>metadata update of existing SIP through GUI editor</li>
 * </ul>
 * <p>
 * Ingest workflow entities created as the result of editing AIP XML in GUI always does not have relation to a {@link Batch}
 * and the state is always {@link IngestWorkflowState#PERSISTED}. If any error occurs during the process the entity is
 * rolled back - deleted completely instead of setting {@link IngestWorkflowState#FAILED}.
 * </p>
 * <p>
 * Ingest workflow entities created the other ways are always related to a {@link Batch} and their state reflects the processing state.
 * </p>
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_ingest_workflow")
@NoArgsConstructor
public class IngestWorkflow extends DatedObject {

    public IngestWorkflow(String id) {
        setId(id);
    }

    private Instant ended;

    /**
     * Externé id
     */
    private String externalId;

    /**
     * Sip balík
     */
    @ManyToOne
    protected Sip sip;

    /**
     * Názov súboru
     */
    protected String fileName;

    /**
     * Stav spracovania
     */
    @Enumerated(EnumType.STRING)
    protected IngestWorkflowState processingState;

    /**
     * Hash balíku dodaný spolu s balíkom
     */
    @ManyToOne(cascade=CascadeType.MERGE)
    protected Hash hash;

    /**
     * Dávka
     */
    @JsonIgnore
    @ManyToOne
    protected Batch batch;

    /**
     * Informácia o dôvode zlyhania
     */
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "ingest_workflow_failure_info_id")
    private IngestWorkflowFailureInfo failureInfo;

    /**
     * Ingest workflow predchádzajúcej ÚSPĚŠNÉ verzie ArclibXml (tj. verze s {@link #processingState} == {@link IngestWorkflowState#PERSISTED})
     */
    @OneToOne
    @JsonIgnore
    private IngestWorkflow relatedWorkflow;

    /**
     * Úroveň verzovania
     */
    @Enumerated(EnumType.STRING)
    private VersioningLevel versioningLevel;

    /**
     * Číslo verzie ArclibXml
     */
    private Integer xmlVersionNumber;

    /**
     * Jedná sa o najnovšiu verziu XML, flag je nastaven až po úspešném přepnutí do stavu {@link IngestWorkflowState#PERSISTED}
     */
    private boolean isLatestVersion;

    /**
     * Hash ArclibXml
     */
    @ManyToOne(cascade=CascadeType.ALL)
    private Hash arclibXmlHash;

    public boolean wasIngestedInDebugMode() {
        return batch != null && batch.isDebuggingModeActive();
    }

    /**
     * returns producer profile which was used to ingest this ingest workflow or its ancestor
     */
    public ProducerProfile getProducerProfile() {
        if (batch != null)
            return batch.getProducerProfile();
        if (relatedWorkflow == null)
            return null;
        return relatedWorkflow.getProducerProfile();
    }
}
