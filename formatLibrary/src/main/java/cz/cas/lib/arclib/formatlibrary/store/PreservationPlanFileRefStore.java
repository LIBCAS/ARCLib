package cz.cas.lib.arclib.formatlibrary.store;

import cz.cas.lib.arclib.domainbase.store.DatedStore;
import cz.cas.lib.arclib.formatlibrary.domain.PreservationPlanFileRef;
import cz.cas.lib.arclib.formatlibrary.domain.QPreservationPlanFileRef;
import org.springframework.stereotype.Repository;

/**
 * Mirrored version of <b>FileRefStore</b> from module <b>system</b>.
 *
 * Implementation of {@link DatedStore} for storing {@link PreservationPlanFileRef}.
 */
@Repository
public class PreservationPlanFileRefStore extends DatedStore<PreservationPlanFileRef, QPreservationPlanFileRef> {
    public PreservationPlanFileRefStore() {
        super(PreservationPlanFileRef.class, QPreservationPlanFileRef.class);
    }
}
