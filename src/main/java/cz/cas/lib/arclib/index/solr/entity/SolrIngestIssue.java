package cz.cas.lib.arclib.index.solr.entity;

import cz.cas.lib.core.index.solr.FieldType;
import cz.cas.lib.core.index.solr.SolrDatedObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

@Getter
@Setter
@SolrDocument(solrCoreName = "ingestIssue")
public class SolrIngestIssue extends SolrDatedObject {

    @Field
    @Indexed(type = FieldType.STRING)
    private String externalId;

    @Field
    @Indexed(type = FieldType.TEXT)
    private String issue;

    @Field
    @Indexed
    private boolean solvedByConfig;

    @Field
    @Indexed(type = FieldType.TEXT)
    private String configNote;
}