package cz.cas.lib.arclib.domain.ingestWorkflow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.domain.VersioningLevel;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.core.domain.DatedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;

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
    protected String originalFileName;

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
     * Ingest workflow predchádzajúcej verzie ArclibXml
     */
    @OneToOne
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
     * Jedná sa o najnovšiu verziu XML
     */
    private boolean isLatestVersion;

    /**
     * Hash ArclibXml
     */
    @ManyToOne(cascade=CascadeType.ALL)
    private Hash arclibXmlHash;
}
