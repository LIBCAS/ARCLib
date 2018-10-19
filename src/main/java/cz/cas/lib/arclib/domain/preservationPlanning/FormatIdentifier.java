package cz.cas.lib.arclib.domain.preservationPlanning;

import cz.cas.lib.core.domain.DomainObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * Identifikátoru formátu
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_format_identifier")
@NoArgsConstructor
public class FormatIdentifier extends DomainObject {
    /**
     * Typ identifikátora formátu
     */
    @Enumerated(EnumType.STRING)
    private FormatIdentifierType type;

    /**
     * Hodnota identifikátora formátu
     */
    private String value;
}
