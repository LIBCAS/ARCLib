package cz.inqool.uas.sequence;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SequenceStoreTest {
    private SequenceStoreImpl store;

    @Before
    public void setUp() {
        store = new SequenceStoreImpl();
    }

    @Test
    public void toIndexObjectTest() {
        Sequence sequence = new Sequence();
        sequence.setCounter(5L);
        sequence.setFormat("'#'#");

        IndexedSequence indexedSequence = store.toIndexObject(sequence);

        assertThat(indexedSequence.getCounter(), is(sequence.getCounter()));
        assertThat(indexedSequence.getFormat(), is(sequence.getFormat()));
    }

    private class SequenceStoreImpl extends SequenceStore {
    }
}
