package cz.cas.lib.core.index;


import cz.cas.lib.core.index.solr.IndexFieldType;
import cz.cas.lib.core.index.solr.IndexedDatedStore;
import cz.cas.lib.core.index.solr.IndexedDomainObject;
import cz.cas.lib.core.index.solr.IndexField;
import cz.cas.lib.core.index.solr.util.TemporalConverters;
import lombok.Getter;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.index.solr.IndexQueryUtils.inQuery;
import static cz.cas.lib.arclib.index.solr.IndexQueryUtils.notInQuery;
import static java.util.Collections.emptySet;

@Repository
public class IndexedStoreImpl extends IndexedDatedStore<TestEntity, QTestEntity, IndexedTestEntity> {
    public IndexedStoreImpl() {
        super(TestEntity.class, QTestEntity.class, IndexedTestEntity.class);
    }

    @Getter
    private final String indexType = "testentity";

    @Override
    public IndexedTestEntity toIndexObject(TestEntity obj) {
        IndexedTestEntity indexObject = super.toIndexObject(obj);

        indexObject.setCustomSortStringAttribute(obj.getTextualAttribute());
        indexObject.setStringAttribute(obj.getTextualAttribute());
        indexObject.setFoldingAttribute(obj.getTextualAttribute());
        indexObject.setTextAttributeWithStringCpyField(obj.getTextualAttribute());
        indexObject.setTextAttribute(obj.getTextualAttribute());

        indexObject.setIntAttribute(obj.getIntAttribute());
        indexObject.setDoubleAttribute(obj.getDoubleAttribute());
        indexObject.setLocalDateAttribute(TemporalConverters.localDateToIsoUtcString(obj.getLocalDateAttribute()));
        indexObject.setInstantAttribute(TemporalConverters.instantToIsoUtcString(obj.getInstantAttribute()));

        return indexObject;
    }

    public List<String> findEmptyInFilter() {

        SolrQuery query = new SolrQuery("*:*");
        IndexField field = new IndexField();
        field.setFieldType(IndexFieldType.STRING);
        field.setFieldName("customSortStringAttribute");
        field.setKeywordField(field.getFieldName());
        query.addFilterQuery(inQuery(field, emptySet()));
        query.addField("id");
        QueryResponse queryResponse;
        try {
            queryResponse = solrClient.query(getIndexCollection(), query);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        List<IndexedTestEntity> beans = queryResponse.getBeans(getUType());
        return beans.stream().map(IndexedDomainObject::getId).collect(Collectors.toList());
    }

    public List<String> findEmptyNotInFilter() {
        IndexField field = new IndexField();
        field.setFieldType(IndexFieldType.STRING);
        field.setFieldName("customSortStringAttribute");
        field.setKeywordField(field.getFieldName());
        SolrQuery query = new SolrQuery("*:*");
        query.addFilterQuery(notInQuery(field, emptySet()));
        query.addField("id");
        QueryResponse queryResponse;
        try {
            queryResponse = solrClient.query(getIndexCollection(), query);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        List<IndexedTestEntity> beans = queryResponse.getBeans(getUType());
        return beans.stream().map(IndexedDomainObject::getId).collect(Collectors.toList());
    }
}
