package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.QProducer;
import cz.cas.lib.core.store.NamedStore;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
public class ProducerStore extends NamedStore<Producer, QProducer> {

    @Transactional
    public Producer findByName(String name) {
        QProducer producer = qObject();

        Producer producerFound = query()
                .select(producer)
                .where(producer.name.eq(name))
                .fetchFirst();

        detachAll();
        return producerFound;
    }

    public ProducerStore() {
        super(Producer.class, QProducer.class);
    }
}
