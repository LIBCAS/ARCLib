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
@SolrDocument(collection = "fileFormat")
public class SolrFormat extends SolrDatedObject {

    @Field
    @Indexed(type = FieldType.INT)
    private Integer formatId;

    @Field
    @Indexed(type = FieldType.STRING)
    private String puid;

    @Field
    @Indexed(type = FieldType.FOLDING)
    private String formatName;

    @Field
    @Indexed(type = FieldType.STRING)
    private String threatLevel;
}
