package cz.cas.lib.arclib.domain.ingestWorkflow;

import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.core.domain.DatedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "arclib_ingest_event")
public class IngestEvent extends DatedObject {
    /**
     * related ingest workflow
     */
    @ManyToOne
    @NotNull
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
}
