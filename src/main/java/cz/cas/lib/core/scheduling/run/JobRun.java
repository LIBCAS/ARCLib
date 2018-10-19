package cz.cas.lib.core.scheduling.run;

import cz.cas.lib.core.domain.DatedObject;
import cz.cas.lib.core.scheduling.job.Job;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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

    @Lob
    private String console;

    @Lob
    private String result;

    private Boolean success;
}
