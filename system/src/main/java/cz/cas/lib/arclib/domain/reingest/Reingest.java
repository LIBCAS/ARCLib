package cz.cas.lib.arclib.domain.reingest;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import cz.cas.lib.core.scheduling.job.Job;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

/**
 * Reingest
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_reingest")
@NoArgsConstructor
public class Reingest extends DatedObject {

    private int nextOffset;

    private int size;

    @Enumerated(EnumType.STRING)
    private ReingestState state;

    @OneToOne
    private Job exporterJob;
}
