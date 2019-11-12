package cz.cas.lib.arclib.domain.ingestWorkflow;


import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinition;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.Arrays;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@NoArgsConstructor
@Table(name = "arclib_ingest_issue")
public class IngestIssue extends IngestEvent {

    public IngestIssue(IngestWorkflow iw, Tool tool, IngestIssueDefinition def, FormatDefinition f, String description, boolean solvedByConfig) {
        super(iw, tool, solvedByConfig, description);
        this.ingestIssueDefinition = def;
        this.formatDefinition = f;
    }

    /**
     * type of the issue
     */
    @ManyToOne
    @NotNull
    private IngestIssueDefinition ingestIssueDefinition;

    /**
     * format of the file during which processing the issue has occurred, null if the issue does not relate to a one specific file of one specific format
     */
    @ManyToOne
    private FormatDefinition formatDefinition;

    public static String createMissingConfigNote(String configPath) {
        return "missing config at: " + configPath;
    }

    public static String createInvalidConfigNote(String configPath, String nodeValue, Class<? extends Enum> supportedValues) {
        return "invalid config: " + nodeValue + " at: " + configPath + " supported values: " + Arrays.toString(supportedValues.getEnumConstants());
    }

    public static String createInvalidConfigNote(String configPath, String nodeValue, String... supportedValues) {
        return "invalid config: " + nodeValue + " at: " + configPath + " supported values: " + Arrays.toString(supportedValues);
    }

    public static String createUsedConfigNote(String configPath, String configValue) {
        return "used config: " + configValue + " at: " + configPath;
    }

    @Override
    public String toString() {
        return "IngestIssue{" +
                "ingestWorkflow=" + super.getIngestWorkflow() +
                ", tool=" + super.getTool() +
                ", ingestIssueDefinition=" + ingestIssueDefinition +
                ", formatDefinition=" + formatDefinition +
                ", issue='" + super.getDescription() + '\'' +
                ", solvedByConfig=" + super.isSuccess() +
                '}';
    }
}
