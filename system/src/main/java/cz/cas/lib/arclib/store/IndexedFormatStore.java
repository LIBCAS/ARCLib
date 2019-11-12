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
public class IndexedFormatStore
        extends IndexedDatedStore<Format, QFormat, IndexedFormat> implements FormatStore {
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

        Integer formatId = obj.getFormatId();
        if (formatId != null) {
            indexObject.setFormatId(formatId);
        }

        String puid = obj.getPuid();
        if (puid != null) {
            indexObject.setPuid(puid);
        }

        String formatName = obj.getFormatName();
        if (formatName != null) {
            indexObject.setFormatName(formatName);
        }
        indexObject.setThreatLevel(obj.getThreatLevel());
        return indexObject;
    }

    @Inject
    public void setDbFormatStore(DbFormatStore dbFormatStore) {
        this.dbFormatStore = dbFormatStore;
    }
}

