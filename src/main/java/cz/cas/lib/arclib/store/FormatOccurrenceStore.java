package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.preservationPlanning.FormatOccurrence;
import cz.cas.lib.arclib.domain.preservationPlanning.QFormatOccurrence;
import cz.cas.lib.core.store.DomainStore;
import cz.cas.lib.core.store.Transactional;
import org.springframework.stereotype.Repository;

@Repository
public class FormatOccurrenceStore
        extends DomainStore<FormatOccurrence, QFormatOccurrence> {
    public FormatOccurrenceStore() {
        super(FormatOccurrence.class, QFormatOccurrence.class);
    }

    @Override
    @Transactional
    public FormatOccurrence save(FormatOccurrence entity) {
        return super.save(entity);
    }

    public FormatOccurrence findByFormatDefinitionAndProducerProfile(String formatDefinitionId, String producerProfileId) {
        QFormatOccurrence qFormatOccurrence = qObject();
        FormatOccurrence entity = query()
                .select(qFormatOccurrence)
                .where(qFormatOccurrence.formatDefinition.id.eq(formatDefinitionId))
                .where(qFormatOccurrence.producerProfile.id.eq(producerProfileId))
                .fetchFirst();
        detachAll();
        return entity;
    }
}

