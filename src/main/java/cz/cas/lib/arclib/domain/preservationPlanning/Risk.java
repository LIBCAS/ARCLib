package cz.cas.lib.arclib.domain.preservationPlanning;

import cz.cas.lib.core.domain.NamedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Riziko viažuce sa ku formátu
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_risk")
@NoArgsConstructor
public class Risk extends NamedObject {
    /**
     * Popis
     */
    private String description;
}
