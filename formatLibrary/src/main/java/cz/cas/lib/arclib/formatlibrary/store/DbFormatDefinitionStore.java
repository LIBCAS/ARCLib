package cz.cas.lib.arclib.formatlibrary.store;

import cz.cas.lib.arclib.domainbase.store.DatedStore;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import cz.cas.lib.arclib.formatlibrary.domain.QFormatDefinition;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DbFormatDefinitionStore
        extends DatedStore<FormatDefinition, QFormatDefinition> implements FormatDefinitionStore {
    public DbFormatDefinitionStore() {
        super(FormatDefinition.class, QFormatDefinition.class);
    }

    public List<FormatDefinition> findByFormatId(Integer formatId, boolean localDefinition) {
        List<FormatDefinition> fetch = query().select(qObject())
                .where(qObject().format.formatId.eq(formatId))
                .where(qObject().localDefinition.eq(localDefinition))
                .orderBy(qObject().created.desc())
                .fetch();
        detachAll();
        return fetch;
    }

    public FormatDefinition findPreferredByFormatId(Integer formatId) {
        FormatDefinition fetch = query().select(qObject())
                .where(qObject().format.formatId.eq(formatId))
                .where(qObject().preferred.eq(true)).fetchFirst();
        detachAll();
        return fetch;
    }

    public FormatDefinition findPreferredByFormatPuid(String puid) {
        FormatDefinition fetch = query().select(qObject())
                .where(qObject().format.puid.eq(puid))
                .where(qObject().preferred.eq(true)).fetchFirst();
        detachAll();
        return fetch;
    }

    @Override
    public FormatDefinition create(FormatDefinition entity) {
        return save(entity);
    }

    @Override
    public FormatDefinition update(FormatDefinition entity) {
        return save(entity);
    }
}
