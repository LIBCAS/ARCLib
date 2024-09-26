package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import cz.cas.lib.arclib.formatlibrary.domain.QFormatDefinition;
import cz.cas.lib.arclib.formatlibrary.store.DbFormatDefinitionStore;
import cz.cas.lib.arclib.formatlibrary.store.FormatDefinitionStore;
import cz.cas.lib.arclib.index.solr.entity.IndexedFormatDefinition;
import cz.cas.lib.core.index.solr.IndexedDatedStore;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
@Primary
public class IndexedFormatDefinitionStore
        extends IndexedDatedStore<FormatDefinition, QFormatDefinition, IndexedFormatDefinition> implements FormatDefinitionStore {
    @Getter
    private final String indexType = "formatDefinition";
    public IndexedFormatDefinitionStore() {
        super(FormatDefinition.class, QFormatDefinition.class, IndexedFormatDefinition.class);
    }

    private DbFormatDefinitionStore dbFormatDefinitionStore;

    public List<FormatDefinition> findByFormatId(Integer formatId, boolean localDefinition) {
        return dbFormatDefinitionStore.findByFormatId(formatId, localDefinition);
    }

    public FormatDefinition findPreferredByFormatId(Integer formatId) {
        return dbFormatDefinitionStore.findPreferredByFormatId(formatId);
    }

    public FormatDefinition findPreferredByFormatPuid(String puid) {
        return dbFormatDefinitionStore.findPreferredByFormatPuid(puid);
    }

    @Override
    public FormatDefinition create(FormatDefinition entity) {
        return save(entity);
    }

    @Override
    public FormatDefinition update(FormatDefinition entity) {
        return save(entity);
    }

    @Override
    public IndexedFormatDefinition toIndexObject(FormatDefinition obj) {
        IndexedFormatDefinition indexObject = super.toIndexObject(obj);


        String formatVersion = obj.getFormatVersion();
        if (formatVersion != null) {
            indexObject.setFormatVersion(formatVersion);
        }

        Integer internalVersionNumber = obj.getInternalVersionNumber();
        if (internalVersionNumber != null) {
            indexObject.setInternalVersionNumber(internalVersionNumber);
        }

        indexObject.setInternalInformationFilled(obj.isInternalInformationFilled());
        indexObject.setLocalDefinition(obj.isLocalDefinition());
        indexObject.setPreferred(obj.isPreferred());

        Integer formatId = obj.getFormat().getFormatId();
        if (formatId != null) {
            indexObject.setFormatId(formatId);
        }

        String puid = obj.getFormat().getPuid();
        if (puid != null) {
            indexObject.setPuid(puid);
        }
        return indexObject;
    }

    @Autowired
    public void setDbFormatDefinitionStore(DbFormatDefinitionStore dbFormatDefinitionStore) {
        this.dbFormatDefinitionStore = dbFormatDefinitionStore;
    }
}

