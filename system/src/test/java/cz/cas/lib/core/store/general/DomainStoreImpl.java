package cz.cas.lib.core.store.general;


import cz.cas.lib.arclib.domainbase.store.DomainStore;

public class DomainStoreImpl extends DomainStore<GeneralTestEntity, QGeneralTestEntity> {

    public DomainStoreImpl() {
        super(GeneralTestEntity.class, QGeneralTestEntity.class);
    }
}
