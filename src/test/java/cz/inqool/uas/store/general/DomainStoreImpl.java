package cz.inqool.uas.store.general;


import cz.inqool.uas.store.DomainStore;

public class DomainStoreImpl extends DomainStore<GeneralTestEntity, QGeneralTestEntity> {

    public DomainStoreImpl() {
        super(GeneralTestEntity.class, QGeneralTestEntity.class);
    }
}
