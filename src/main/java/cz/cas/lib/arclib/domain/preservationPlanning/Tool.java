package cz.cas.lib.arclib.domain.preservationPlanning;

import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.core.domain.NamedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.Set;

/**
 *
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_tool")
@NoArgsConstructor
public class Tool extends NamedObject {
    /**
     * Verzia
     */
    private String version;

    /**
     * Popis
     */
    private String description;

    /**
     * True pro tool reprezentujici ARLib IW delegát např. {@link cz.cas.lib.arclib.bpm.ValidatorDelegate}),
     * false pro externí tool, např. DROID
     */
    private boolean internal;

    /**
     * Funkcia
     */
    @Enumerated(EnumType.STRING)
    private IngestToolFunction toolFunction;

    /**
     * Informácia o licencii
     */
    private String licenseInformation;

    /**
     * Dokumentácia
     */
    private String documentation;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "arclib_tool_ingest_issue_definition", joinColumns = {
            @JoinColumn(name = "tool_id", nullable = false, updatable = false)},
            inverseJoinColumns = {@JoinColumn(name = "ingest_issue_definition_id",
                    nullable = false, updatable = false)})
    private Set<IngestIssueDefinition> possibleIssues;


    @Enumerated(EnumType.STRING)
    private FormatRelationType formatRelationType;

    private String formatRelationValue;

    public enum FormatRelationType{
        ALL
    }
}
