package cz.cas.lib.arclib.domain.ingestWorkflow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import cz.cas.lib.arclib.service.tableexport.ExportableTable;
import cz.cas.lib.arclib.service.tableexport.TableDataType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "arclib_ingest_event")
public class IngestEvent extends DatedObject implements ExportableTable {
    /**
     * related ingest workflow
     */
    @ManyToOne
    @NotNull
    @JsonIgnore
    private IngestWorkflow ingestWorkflow;

    /**
     * tool used when the event has occurred
     */
    @ManyToOne
    @NotNull
    private Tool tool;

    /**
     * whether the event was successful
     *
     * <p>
     * for the subclass {@link IngestIssue} this field says
     * whether the JSON config contained a valid value to solve this issue when it has occurred..
     * note that if the config value says, for example, to stop the ingest workflow, the exception may be thrown, and
     * that is still a valid solution by the config.. hence <b>this field is only set to false if the CONFIG does not
     * contain any value or the value cannot be parsed</b>
     * </p>
     */
    private boolean success;

    /**
     * description of the event,
     * may contain additional information related to the JSON config used during the event occurrence.. e.g. "<em>config value ignoreError was set to false</em>"
     * or "<em>config value ignoreError was not parsable, contained value: 'truth' expecting values 'true/false'</em>"
     */
    @Column(length = 10485760)
    private String description;

    @Override
    public Object getExportTableValue(String col) {
        return switch (col) {
            case "created" -> created;
            case "tool" -> tool == null ? null : tool.getName();
            case "toolFunction" -> tool == null ? null : tool.getToolFunction();
            case "description" -> description;
            case "success" -> success;
            default -> null;
        };
    }

    public static List<TableDataType> getExportTableConfig(List<String> columns) {
        return columns.stream().map(IngestEvent::getExportTableConfig).collect(Collectors.toList());
    }

    private static TableDataType getExportTableConfig(String col) {
        return switch (col) {
            case "description", "success" -> TableDataType.OTHER;
            case "created" -> TableDataType.DATE_TIME;
            case "tool", "toolFunction" -> TableDataType.STRING_AUTO_SIZE;
            default -> throw new UnsupportedOperationException("unsupported export column: " + col);
        };
    }
}
