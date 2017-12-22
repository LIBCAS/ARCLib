package cz.inqool.uas.scheduling.job;

import cz.inqool.uas.domain.DatedObject;
import cz.inqool.uas.script.ScriptType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;

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

    @Lob
    private String script;

    private Boolean active;
}
