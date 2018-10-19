package cz.cas.lib.arclib.domain.ingestWorkflow;


import cz.cas.lib.core.domain.DatedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Arrays;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "arclib_ingest_issue")
public class IngestIssue extends DatedObject {

    @ManyToOne
    private IngestWorkflow ingestWorkflow;

    private Class<?> taskExecutor;

    @Column(length = 10485760)
    private String issue;

    private boolean solvedByConfig;

    @Column(length = 10485760)
    private String configNote;

    public IngestIssue(IngestWorkflow ingestWorkflow, Class<?> taskExecutor, String issue) {
        this.ingestWorkflow = ingestWorkflow;
        this.issue = issue;
        this.taskExecutor = taskExecutor;
    }

    public void setMissingConfigNote(String configPath) {
        this.configNote = "missing config at: " + configPath;
    }

    public void setInvalidConfigNote(String configPath, String nodeValue, Class<? extends Enum> supportedValues) {
        this.configNote = "invalid config: " + nodeValue + " at: " + configPath + " supported values: " + Arrays.toString(supportedValues.getEnumConstants());
    }

    public void setInvalidConfigNote(String configPath, String nodeValue, String... supportedValues) {
        this.configNote = "invalid config: " + nodeValue + " at: " + configPath + " supported values: " + Arrays.toString(supportedValues);
    }

    public void setUsedConfigNote(String configPath, String configValue) {
        this.configNote = "used config: " + configValue + " at: " + configPath;
    }

    @Override
    public String toString() {
        return
                "IngestIssue{" +
                        "ingestWorkflow=" + ingestWorkflow.getExternalId() +
                        ", taskExecutor=" + taskExecutor +
                        ", issue='" + issue + '\'' +
                        ", solvedByConfig=" + solvedByConfig +
                        ", configNote='" + configNote + '\'' +
                        '}';
    }
}
