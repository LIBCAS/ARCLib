package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.QProducer;
import cz.cas.lib.arclib.domainbase.store.NamedStore;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
public class ProducerStore extends NamedStore<Producer, QProducer> {

    @Override
    @Transactional
    public Producer save(Producer entity) {
        return super.save(entity);
    }

    public ProducerStore() {
        super(Producer.class, QProducer.class);
    }
}
