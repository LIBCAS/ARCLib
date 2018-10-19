package cz.cas.lib.core.store.dated;


import cz.cas.lib.core.store.DatedStore;

public class DatedStoreImpl extends DatedStore<DatedTestEntity, QDatedTestEntity> {

    public DatedStoreImpl() {
        super(DatedTestEntity.class, QDatedTestEntity.class);
    }
}
