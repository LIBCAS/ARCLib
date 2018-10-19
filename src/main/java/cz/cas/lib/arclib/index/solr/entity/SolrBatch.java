package cz.cas.lib.arclib.index.solr.entity;

import cz.cas.lib.arclib.domain.BatchState;
import cz.cas.lib.core.index.solr.FieldType;
import cz.cas.lib.core.index.solr.SolrDatedObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

@Getter
@Setter
@SolrDocument(solrCoreName = "batch")
public class SolrBatch extends SolrDatedObject {

    @Field
    @Indexed(type = FieldType.STRING)
    private BatchState state;

    @Field
    @Indexed(type = FieldType.TEXT)
    private String config;

    @Field
    @Indexed(type = FieldType.STRING)
    private String producerId;

    @Field
    @Indexed(type = FieldType.FOLDING)
    private String producerName;

    @Field
    @Indexed(type = FieldType.STRING)
    private String producerProfileId;

    @Field
    @Indexed(type = FieldType.FOLDING)
    private String producerProfileName;

    @Field
    @Indexed(type = FieldType.STRING)
    private String userId;
}
