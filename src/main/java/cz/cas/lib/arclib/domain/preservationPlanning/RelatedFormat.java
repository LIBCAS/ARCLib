package cz.cas.lib.arclib.domain.preservationPlanning;

import cz.cas.lib.core.domain.DomainObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Vzťah medzi formátmi
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_related_format")
@NoArgsConstructor
public class RelatedFormat extends DomainObject {

    /**
     * Typ vťahu
     */
    private String relationshipType;

    /**
     * Súvisiaci formát
     */
    @ManyToOne
    private Format relatedFormat;
}
