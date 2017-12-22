package cz.inqool.uas.index.missing;

import cz.inqool.uas.index.DependentEntity;
import cz.inqool.uas.index.IndexedDatedStore;
import cz.inqool.uas.index.QTestEntity;
import cz.inqool.uas.index.TestEntity;

import static cz.inqool.uas.util.Utils.toLabeledReference;

public class MissingStoreImpl extends IndexedDatedStore<TestEntity, QTestEntity, IndexedTestEntityWrong> {
    public MissingStoreImpl() {
        super(TestEntity.class, QTestEntity.class, IndexedTestEntityWrong.class);
    }

    @Override
    public IndexedTestEntityWrong toIndexObject(TestEntity obj) {
        IndexedTestEntityWrong indexObject = super.toIndexObject(obj);

        indexObject.setStringAttribute(obj.getStringAttribute());
        indexObject.setIntAttribute(obj.getIntAttribute());
        indexObject.setDoubleAttribute(obj.getDoubleAttribute());
        indexObject.setLocalDateAttribute(obj.getLocalDateAttribute());
        indexObject.setInstantAttribute(obj.getInstantAttribute());
        indexObject.setDependent(toLabeledReference(obj.getDependent(), DependentEntity::getName));

        return indexObject;
    }
}
