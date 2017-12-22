package cz.inqool.uas.store.revision;

import cz.inqool.uas.store.HistoricStore;

public class RevisionHistoricStoreImpl extends HistoricStore<VersionedEntity> {
    public RevisionHistoricStoreImpl() {
        super(VersionedEntity.class);
    }

    @Override
    protected void loadEntity(VersionedEntity entity) {
        if (entity != null) {
            entity.getDeps().size();
        }
    }
}
