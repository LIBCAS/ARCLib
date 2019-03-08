package cz.cas.lib.arclib.domain.profiles;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.core.domain.NamedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;

/**
 * Profil dodávateľa
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_producer_profile")
@AllArgsConstructor
@NoArgsConstructor
public class ProducerProfile extends NamedObject {

    public ProducerProfile(String id){
        this.id=id;
    }

    /**
     * Externé id
     */
    private String externalId;

    /**
     * Dodávateľ
     */
    @ManyToOne
    private Producer producer;

    /**
     * Validačný profil
     */
    @ManyToOne
    private ValidationProfile validationProfile;

    /**
     * SIP profil
     */
    @ManyToOne
    private SipProfile sipProfile;

    /**
     * JSON konfigurácia ingest workflow
     */
    @Column(length = 10485760)
    private String workflowConfig;

    /**
     * Definícia ingest workflow vo formáte BPMN
     */
    @ManyToOne(cascade = CascadeType.PERSIST)
    private WorkflowDefinition workflowDefinition;

    /**
     * Aktivovaný režim ladenia profilu dodávateľa
     */
    private boolean debuggingModeActive;
}
