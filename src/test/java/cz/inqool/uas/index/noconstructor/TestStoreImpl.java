package cz.inqool.uas.index.noconstructor;

import cz.inqool.uas.index.DependentEntity;
import cz.inqool.uas.index.IndexedDatedStore;
import cz.inqool.uas.index.QTestEntity;
import cz.inqool.uas.index.TestEntity;

import static cz.inqool.uas.util.Utils.toLabeledReference;

public class TestStoreImpl extends IndexedDatedStore<TestEntity, QTestEntity, IndexedTestEntity> {
    public TestStoreImpl() {
        super(TestEntity.class, QTestEntity.class, IndexedTestEntity.class);
    }

    @Override
    public IndexedTestEntity toIndexObject(TestEntity obj) {
        IndexedTestEntity indexObject = super.toIndexObject(obj);

        indexObject.setStringAttribute(obj.getStringAttribute());
        indexObject.setIntAttribute(obj.getIntAttribute());
        indexObject.setDoubleAttribute(obj.getDoubleAttribute());
        indexObject.setLocalDateAttribute(obj.getLocalDateAttribute());
        indexObject.setInstantAttribute(obj.getInstantAttribute());
        indexObject.setDependent(toLabeledReference(obj.getDependent(), DependentEntity::getName));

        return indexObject;
    }
}
