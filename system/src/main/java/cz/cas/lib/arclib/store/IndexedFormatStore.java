package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.formatlibrary.domain.QFormat;
import cz.cas.lib.arclib.formatlibrary.store.DbFormatStore;
import cz.cas.lib.arclib.formatlibrary.store.FormatStore;
import cz.cas.lib.arclib.index.solr.entity.IndexedFormat;
import cz.cas.lib.core.index.solr.IndexedDatedStore;
import lombok.Getter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.util.List;

@Repository
@Primary
public class IndexedFormatStore extends IndexedDatedStore<Format, QFormat, IndexedFormat> implements FormatStore {
    public IndexedFormatStore() {
        super(Format.class, QFormat.class, IndexedFormat.class);
    }

    @Getter
    private final String indexType = "format";
    private DbFormatStore dbFormatStore;

    public Format findByFormatId(Integer formatId) {
        return dbFormatStore.findByFormatId(formatId);
    }

    @Override
    public Format create(Format entity) {
        return save(entity);
    }

    @Override
    public Format update(Format entity) {
        return save(entity);
    }

    @Override
    public List<Format> findFormatsOfRisk(String riskId) {
        return dbFormatStore.findFormatsOfRisk(riskId);
    }

    @Override
    public IndexedFormat toIndexObject(Format obj) {
        IndexedFormat indexObject = super.toIndexObject(obj);

        if (obj.getFormatName() != null) {
            // Manually set AutoCompleteAware field because Format class is not allowed to implement AutoCompleteAware interface
            indexObject.setAutoCompleteLabel(obj.getFormatName() + " (" + obj.getPuid() + ")");

            indexObject.setFormatName(obj.getFormatName());
        }

        Integer formatId = obj.getFormatId();
        if (formatId != null) {
            indexObject.setFormatId(formatId);
        }

        String puid = obj.getPuid();
        if (puid != null) {
            indexObject.setPuid(puid);
        }

        indexObject.setThreatLevel(obj.getThreatLevel());
        return indexObject;
    }

    /**
     * Manually override AutoCompleteAware interface check
     * because Format class cannot implement this interface but still needs to support AutoComplete searches.
     */
    @Override
    public boolean isAutoCompleteSearchAllowed() {
        return true;
    }

    @Inject
    public void setDbFormatStore(DbFormatStore dbFormatStore) {
        this.dbFormatStore = dbFormatStore;
    }
}

