package cz.cas.lib.arclib.domain.preservationPlanning;

import cz.cas.lib.core.domain.NamedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;

/**
 * Položka v číselníku chyb
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_ingest_issue_definition")
@NoArgsConstructor
public class IngestIssueDefinition extends NamedObject {

    /**
     * Sekvenční číselné označení
     */
    @Column(unique = true)
    private String number;

    /**
     * Název
     */
    private String name;

    /**
     * Aplikační kód chyby
     */
    @Column(unique = true)
    @Enumerated(EnumType.STRING)
    private IngestIssueDefinitionCode code;

    /**
     * Popis
     */
    private String description;

    /**
     * Řešení
     */
    private String solution;

    /**
     * Může být řešena změnou v JSON konfiguraci
     */
    private boolean reconfigurable;

    @Override
    public String toString() {
        return "IngestIssueDefinition{" +
                "number='" + number + '\'' +
                ", name='" + name + '\'' +
                ", code=" + code +
                '}';
    }
}
