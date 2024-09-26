package cz.cas.lib.arclib.index.solr.entity;

import cz.cas.lib.arclib.formatlibrary.domain.ThreatLevel;
import cz.cas.lib.core.index.Indexed;
import cz.cas.lib.core.index.SolrDocument;
import cz.cas.lib.core.index.solr.IndexFieldType;
import cz.cas.lib.core.index.solr.IndexedDatedObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;

@Getter
@Setter
@SolrDocument(collection = "formatC")
public class IndexedFormat extends IndexedDatedObject {

    @Field
    @Indexed(type = IndexFieldType.INT)
    private Integer formatId;

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private String puid;

    @Field
    @Indexed(type = IndexFieldType.FOLDING)
    private String formatName;

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private ThreatLevel threatLevel;
}
