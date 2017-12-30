package cz.inqool.uas.store.dated;


import cz.inqool.uas.store.DomainStore;

public class DatedStoreDomainImpl extends DomainStore<DatedTestEntity, QDatedTestEntity> {

    public DatedStoreDomainImpl() {
        super(DatedTestEntity.class, QDatedTestEntity.class);
    }
}
