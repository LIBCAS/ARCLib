package cz.cas.lib.core.index;


import cz.cas.lib.core.index.solr.SolrDatedStore;
import cz.cas.lib.core.index.solr.SolrDomainObject;
import cz.cas.lib.core.index.solr.util.TemporalConverters;
import org.springframework.data.domain.Page;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleQuery;

import java.util.List;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.index.solr.SolrQueryUtils.inQuery;
import static cz.cas.lib.arclib.index.solr.SolrQueryUtils.notInQuery;
import static cz.cas.lib.core.util.Utils.toSolrReference;
import static java.util.Collections.emptySet;

public class SolrStoreImpl extends SolrDatedStore<TestEntity, QTestEntity, SolrTestEntity> {
    public SolrStoreImpl() {
        super(TestEntity.class, QTestEntity.class, SolrTestEntity.class);
    }

    @Override
    public SolrTestEntity toIndexObject(TestEntity obj) {
        SolrTestEntity indexObject = super.toIndexObject(obj);

        indexObject.setStringAttribute(obj.getStringAttribute());
        indexObject.setFoldingAttribute(obj.getStringAttribute());

        indexObject.setIntAttribute(obj.getIntAttribute());
        indexObject.setDoubleAttribute(obj.getDoubleAttribute());
        indexObject.setLocalDateAttribute(TemporalConverters.localDateToIsoUtcString(obj.getLocalDateAttribute()));
        indexObject.setInstantAttribute(TemporalConverters.instantToIsoUtcString(obj.getInstantAttribute()));
        indexObject.setDependent(toSolrReference(obj.getDependent(), DependentEntity::getName));

//        if (obj.getDependents() != null) {
//            Set<SolrReference> references = obj.getDependents().stream()
//                    .map(d -> toSolrReference(d, DependentEntity::getName))
//                    .collect(Collectors.toSet());
//            indexObject.setDependents(references);
//        }

        return indexObject;
    }

    public List<String> findEmptyInFilter() {

        SimpleQuery query = new SimpleQuery();
        query.addFilterQuery(new SimpleFilterQuery(inQuery("stringAttribute", emptySet())));
        query.addProjectionOnField("id");
        query.addCriteria(Criteria.where("id"));
        Page<SolrTestEntity> page = template.query(getIndexCore(), query, getUType());
        return page.getContent().stream().map(SolrDomainObject::getId).collect(Collectors.toList());
    }

    public List<String> findEmptyNotInFilter() {
        SimpleQuery query = new SimpleQuery();
        query.addFilterQuery(new SimpleFilterQuery(notInQuery("stringAttribute", emptySet())));
        query.addProjectionOnField("id");
        query.addCriteria(Criteria.where("id"));
        Page<SolrTestEntity> page = template.query(getIndexCore(), query, getUType());
        return page.getContent().stream().map(SolrDomainObject::getId).collect(Collectors.toList());
    }
}
