package cz.cas.lib.arclib.index.solr.entity;

import cz.cas.lib.core.index.solr.IndexFieldType;
import cz.cas.lib.core.index.solr.IndexedDatedObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

@Getter
@Setter
@SolrDocument(collection = "ingestIssueC")
public class IndexedIngestIssue extends IndexedDatedObject {

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private String externalId;

    @Field
    @Indexed(type = IndexFieldType.TEXT)
    private String issue;

    @Field
    @Indexed(type = IndexFieldType.BOOLEAN)
    private boolean solvedByConfig;

    @Field
    @Indexed(type = IndexFieldType.FOLDING)
    private String toolName;

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private String toolId;

    @Field
    @Indexed(type = IndexFieldType.FOLDING)
    private String ingestIssueDefinitionName;

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private String ingestIssueDefinitionId;

    @Field
    @Indexed(type = IndexFieldType.FOLDING)
    private String formatName;

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private String formatPuid;
}
