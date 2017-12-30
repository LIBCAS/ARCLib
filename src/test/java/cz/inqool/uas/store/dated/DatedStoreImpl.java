package cz.inqool.uas.store.dated;


import cz.inqool.uas.store.DatedStore;

public class DatedStoreImpl extends DatedStore<DatedTestEntity, QDatedTestEntity> {

    public DatedStoreImpl() {
        super(DatedTestEntity.class, QDatedTestEntity.class);
    }
}
