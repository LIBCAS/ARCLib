package cz.cas.lib.core.sequence;

import cz.cas.lib.arclib.domainbase.store.DatedStore;
import org.springframework.stereotype.Repository;

@Repository
public class SequenceStore extends DatedStore<Sequence, QSequence> {

    public SequenceStore() {
        super(Sequence.class, QSequence.class);
    }
}
