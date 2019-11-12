package cz.cas.lib.arclib.index.solr.entity;

import cz.cas.lib.arclib.domain.BatchState;
import cz.cas.lib.core.index.solr.IndexFieldType;
import cz.cas.lib.core.index.solr.IndexedDatedObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

@Getter
@Setter
@SolrDocument(collection = "arclibDomainC")
public class IndexedBatch extends IndexedDatedObject {

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private BatchState state;

    @Field
    @Indexed(type = IndexFieldType.TEXT)
    private String config;

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private String producerId;

    @Field
    @Indexed(type = IndexFieldType.FOLDING, copyTo = {"producerName_sort"})
    private String producerName;

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private String producerProfileId;

    @Field
    @Indexed(type = IndexFieldType.FOLDING, copyTo = {"producerProfileName_sort"})
    private String producerProfileName;

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private String userId;

    @Field
    @Indexed(type = IndexFieldType.BOOLEAN)
    private Boolean pendingIncidents;
}
