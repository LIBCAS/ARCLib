package cz.cas.lib.core.bpm.association;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Transient;
import java.util.Set;

/**
 * Holds information about processes and tasks where this object is currently processed.
 */
@Getter
@Setter
public class BpmObject extends DatedObject {
    @Transient
    private Set<BpmAssociation> bpmAssociations;
}
