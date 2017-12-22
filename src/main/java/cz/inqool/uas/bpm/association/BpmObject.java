package cz.inqool.uas.bpm.association;

import cz.inqool.uas.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Transient;
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
