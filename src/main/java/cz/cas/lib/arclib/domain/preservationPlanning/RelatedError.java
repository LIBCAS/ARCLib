package cz.cas.lib.arclib.domain.preservationPlanning;

import cz.cas.lib.core.domain.DatedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Súvisiaca chyba
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_related_error")
@NoArgsConstructor
public class RelatedError extends DatedObject {
    /**
     * Popis
     */
    private String description;

    /**
     * Riešenie
     */
    private String solution;
}
