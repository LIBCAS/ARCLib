package cz.cas.lib.arclib.index.solr.entity;

import cz.cas.lib.core.index.Indexed;
import cz.cas.lib.core.index.SolrDocument;
import cz.cas.lib.core.index.solr.IndexFieldType;
import cz.cas.lib.core.index.solr.IndexedDatedObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;

@Getter
@Setter
@SolrDocument(collection = "arclibDomainC")
public class IndexedBatch extends IndexedDatedObject {

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private String state;

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
    private String userId;

    @Field
    @Indexed(type = IndexFieldType.BOOLEAN)
    private Boolean pendingIncidents;

    @Field
    @Indexed(type = IndexFieldType.FOLDING)
    private String producerProfile;
    @Field
    @Indexed(type = IndexFieldType.FOLDING)
    private String initialSipProfile;
    @Field
    @Indexed(type = IndexFieldType.FOLDING)
    private String initialWorkflowDefinition;
    @Field
    @Indexed(type = IndexFieldType.FOLDING)
    private String initialValidationProfile;
}
