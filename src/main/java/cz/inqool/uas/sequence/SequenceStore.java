package cz.inqool.uas.sequence;

import cz.inqool.uas.index.IndexedDictionaryStore;
import cz.inqool.uas.index.IndexedStore;
import org.springframework.stereotype.Repository;

/**
 * Implementation of {@link IndexedStore} for storing {@link Sequence} and indexing {@link IndexedSequence}.
 */
@Repository
public class SequenceStore extends IndexedDictionaryStore<Sequence, QSequence, IndexedSequence> {

    public SequenceStore() {
        super(Sequence.class, QSequence.class, IndexedSequence.class);
    }

    @Override
    public IndexedSequence toIndexObject(Sequence o) {
        IndexedSequence indexedFileRef = super.toIndexObject(o);

        indexedFileRef.setFormat(o.getFormat());
        indexedFileRef.setCounter(o.getCounter());

        return indexedFileRef;
    }
}
