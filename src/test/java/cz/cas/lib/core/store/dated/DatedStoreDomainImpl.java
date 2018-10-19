package cz.cas.lib.core.store.dated;


import cz.cas.lib.core.store.DomainStore;

public class DatedStoreDomainImpl extends DomainStore<DatedTestEntity, QDatedTestEntity> {

    public DatedStoreDomainImpl() {
        super(DatedTestEntity.class, QDatedTestEntity.class);
    }
}
