package cz.cas.lib.core.index.missing;

import cz.cas.lib.core.index.DependentEntity;
import cz.cas.lib.core.index.QTestEntity;
import cz.cas.lib.core.index.TestEntity;
import cz.cas.lib.core.index.solr.SolrDatedStore;
import cz.cas.lib.core.index.solr.util.TemporalConverters;

import static cz.cas.lib.core.util.Utils.toSolrReference;

public class MissingStoreImpl extends SolrDatedStore<TestEntity, QTestEntity, SolrTestEntityWrong> {
    public MissingStoreImpl() {
        super(TestEntity.class, QTestEntity.class, SolrTestEntityWrong.class);
    }

    @Override
    public SolrTestEntityWrong toIndexObject(TestEntity obj) {
        SolrTestEntityWrong indexObject = super.toIndexObject(obj);

        indexObject.setStringAttribute(obj.getStringAttribute());
        indexObject.setIntAttribute(obj.getIntAttribute());
        indexObject.setDoubleAttribute(obj.getDoubleAttribute());
        indexObject.setLocalDateAttribute(TemporalConverters.localDateToIsoUtcString(obj.getLocalDateAttribute()));
        indexObject.setInstantAttribute(TemporalConverters.instantToIsoUtcString(obj.getInstantAttribute()));
        indexObject.setDependent(toSolrReference(obj.getDependent(), DependentEntity::getName));

        return indexObject;
    }
}
