package cz.inqool.uas.store.revision;


import cz.inqool.uas.store.DomainStore;

public class RevisionStoreImpl extends DomainStore<VersionedEntity, QVersionedEntity> {

    public RevisionStoreImpl() {
        super(VersionedEntity.class, QVersionedEntity.class);
    }
}
