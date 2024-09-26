package cz.cas.lib.core.scheduling.job;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import cz.cas.lib.core.script.ScriptType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.*;
import java.util.Map;

/**
 * Time based job
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "uas_job")
public class Job extends DatedObject {
    private String name;

    private String timing;

    /**
     * Language of the script used
     */
    @Enumerated(EnumType.STRING)
    private ScriptType scriptType;

    @Column(length = 10485760)
    private String script;

    private Boolean active;

    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "uas_job_params")
    @MapKeyColumn(name = "key")
    @Column(name = "value", length = 10485760)
    private Map<String, String> params;
}
