package cz.cas.lib.arclib.domain.preservationPlanning;

import cz.cas.lib.core.domain.NamedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

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
    private Integer version;

    /**
     * Popis
     */
    private String description;

    /**
     * Funkcia
     */
    @Enumerated(EnumType.STRING)
    private ToolFunction toolFunction;

    /**
     * Informácia o licencii
     */
    private String licenseInformation;

    /**
     * Dokumentácia
     */
    private String documentation;
}
