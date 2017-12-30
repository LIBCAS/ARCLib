package cz.inqool.uas.sequence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.MissingObject;
import helper.DbTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class GeneratorTest extends DbTest {
    private SequenceStore store;

    private Generator generator;

    @Mock
    private ElasticsearchTemplate template;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        store = new SequenceStore();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));
        store.setTemplate(template);

        generator = new Generator();
        generator.setStore(store);
    }

    @Test
    public void missingSequenceId() {
        assertThrown(() -> generator.generate(null))
                .isInstanceOf(BadArgument.class);

        assertThrown(() -> generator.generate(null, 1L))
                .isInstanceOf(BadArgument.class);
    }

    @Test
    public void missingSequence() {
        assertThrown(() -> generator.generate("fake"))
                .isInstanceOf(MissingObject.class);

        assertThrown(() -> generator.generate("fake", 1L))
                .isInstanceOf(MissingObject.class);
    }

    @Test
    public void generateWithoutCounter() {
        Sequence sequence = new Sequence();
        sequence.setCounter(2L);
        sequence.setFormat("'#'#");

        store.save(sequence);
        flushCache();

        String number = generator.generate(sequence.getId());
        assertThat(number, is("#2"));

        sequence = store.find(sequence.getId());
        assertThat(sequence.getCounter(), is(3L));
    }

    @Test
    public void generateWithoutCounterNulled() {
        Sequence sequence = new Sequence();
        sequence.setCounter(null);
        sequence.setFormat("'#'#");

        store.save(sequence);
        flushCache();

        String number = generator.generate(sequence.getId());
        assertThat(number, is("#1"));

        sequence = store.find(sequence.getId());
        assertThat(sequence.getCounter(), is(2L));
    }

    @Test
    public void generateWithNullCounter() {
        Sequence sequence = new Sequence();
        sequence.setCounter(2L);
        sequence.setFormat("'#'#");

        store.save(sequence);
        flushCache();

        assertThrown(() -> generator.generate(sequence.getId(), null))
                .isInstanceOf(BadArgument.class);
    }

    @Test
    public void generateWithCounter() {
        Sequence sequence = new Sequence();
        sequence.setCounter(2L);
        sequence.setFormat("'#'#");

        store.save(sequence);
        flushCache();

        String number = generator.generate(sequence.getId(), 5L);
        assertThat(number, is("#5"));

        sequence = store.find(sequence.getId());
        assertThat(sequence.getCounter(), is(6L));
    }
}
