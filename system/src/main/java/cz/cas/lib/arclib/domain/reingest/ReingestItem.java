package cz.cas.lib.arclib.domain.reingest;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_reingest_item")
@NoArgsConstructor
@AllArgsConstructor
public class ReingestItem extends DomainObject {

    @ManyToOne
    private Reingest reingest;

    @ManyToOne
    private IngestWorkflow ingestWorkflow;
    
    @ManyToOne
    private ProducerProfile producerProfile;
    
    private String configMd5;
}
