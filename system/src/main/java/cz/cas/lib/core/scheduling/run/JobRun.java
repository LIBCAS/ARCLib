package cz.cas.lib.core.scheduling.run;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import cz.cas.lib.core.scheduling.job.Job;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Previous run of job
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "uas_job_run")
public class JobRun extends DatedObject {
    @Fetch(FetchMode.SELECT)
    @ManyToOne
    private Job job;

    private String console;

    private String result;

    private Boolean success;
}
