package cz.cas.lib.core.index.noconstructor;

import cz.cas.lib.core.index.DependentEntity;
import cz.cas.lib.core.index.QTestEntity;
import cz.cas.lib.core.index.TestEntity;
import cz.cas.lib.core.index.solr.SolrDatedStore;
import cz.cas.lib.core.index.solr.util.TemporalConverters;

import static cz.cas.lib.core.util.Utils.toSolrReference;


public class TestStoreImpl extends SolrDatedStore<TestEntity, QTestEntity, SolrTestEntity> {
    public TestStoreImpl() {
        super(TestEntity.class, QTestEntity.class, SolrTestEntity.class);
    }

    @Override
    public SolrTestEntity toIndexObject(TestEntity obj) {
        SolrTestEntity indexObject = super.toIndexObject(obj);

        indexObject.setStringAttribute(obj.getStringAttribute());
        indexObject.setIntAttribute(obj.getIntAttribute());
        indexObject.setDoubleAttribute(obj.getDoubleAttribute());
        indexObject.setLocalDateAttribute(TemporalConverters.localDateToIsoUtcString(obj.getLocalDateAttribute()));
        indexObject.setInstantAttribute(TemporalConverters.instantToIsoUtcString(obj.getInstantAttribute()));
        indexObject.setDependent(toSolrReference(obj.getDependent(), DependentEntity::getName));

        return indexObject;
    }
}
