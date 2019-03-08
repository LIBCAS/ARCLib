package cz.cas.lib.core.scheduling.job;

import cz.cas.lib.core.index.solr.FieldType;
import cz.cas.lib.core.index.solr.SolrDatedObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

/**
 * Indexed representation of {@link Job}.
 */
@Getter
@Setter
@SolrDocument(collection = "job")
public class SolrJob extends SolrDatedObject {
    @Field
    @Indexed(type = FieldType.FOLDING)
    protected String name;

    @Field
    @Indexed
    protected String timing;

    @Field(child = true)
    @Indexed(readonly = true)
    protected String scriptTypeName;

    @Field
    @Indexed
    protected Boolean active;
}
