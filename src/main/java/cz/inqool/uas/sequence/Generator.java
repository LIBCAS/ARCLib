package cz.inqool.uas.sequence;

import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.MissingObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.text.DecimalFormat;

import static cz.inqool.uas.util.Utils.notNull;

/**
 * Number sequence generator.
 */
@Service
public class Generator {
    private SequenceStore store;

    /**
     * Generates next non formatted number from {@link Sequence}.
     *
     * <p>
     *     For this method to be tread-safe and not generate same number for two calls, this method is synchronized
     *     and requires a new transaction explicitly.
     * </p>
     *
     * @param id Id of the {@link Sequence}
     * @return Non Formatted number
     * @throws MissingObject If the {@link Sequence} does not exist
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized long generatePlain(String id) throws MissingObject {
        notNull(id, () -> new BadArgument("id"));

        Sequence sequence = store.find(id);
        notNull(sequence, () -> new MissingObject(Sequence.class, id));

        Long counter = sequence.getCounter();
        if (counter == null) {
            counter = 1L;
        }

        sequence.setCounter(counter+1);
        store.save(sequence);

        return counter;
    }

    /**
     * Generates next formatted number from {@link Sequence}.
     *
     * <p>
     *     For this method to be tread-safe and not generate same number for two calls, this method is synchronized
     *     and requires a new transaction explicitly.
     * </p>
     *
     * @param id Id of the {@link Sequence}
     * @return Formatted number
     * @throws MissingObject If the {@link Sequence} does not exist
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized String generate(String id) throws MissingObject {
        notNull(id, () -> new BadArgument("id"));

        Sequence sequence = store.find(id);
        notNull(sequence, () -> new MissingObject(Sequence.class, id));

        Long counter = sequence.getCounter();
        if (counter == null) {
            counter = 1L;
        }

        sequence.setCounter(counter+1);
        store.save(sequence);

        DecimalFormat format = new DecimalFormat(sequence.getFormat());
        return format.format(counter);
    }

    /**
     * Generates next formatted number from {@link Sequence} using the provided input counter.
     *
     * <p>
     *     Does not use current counter stored in database to determine next input number. But the specified counter + 1
     *     is saved after generation as the next counter.
     * </p>
     * <p>
     *     For this method to be tread-safe and not generate same number for two calls, this method is synchronized
     *     and requires a new transaction explicitly.
     * </p>
     *
     * @param id Id of the {@link Sequence}
     * @param counter Provided counter
     * @return Formatted number
     * @throws MissingObject If the {@link Sequence} does not exist
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized String generate(String id, Long counter) throws MissingObject {
        notNull(id, () -> new BadArgument("id"));
        notNull(counter, () -> new BadArgument("counter"));

        Sequence sequence = store.find(id);
        notNull(sequence, () -> new MissingObject(Sequence.class, id));

        sequence.setCounter(counter+1);
        store.save(sequence);

        DecimalFormat format = new DecimalFormat(sequence.getFormat());
        return format.format(counter);
    }

    @Inject
    public void setStore(SequenceStore store) {
        this.store = store;
    }
}
