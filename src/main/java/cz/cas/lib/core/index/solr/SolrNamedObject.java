package cz.cas.lib.core.index.solr;

import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;

@Getter
@Setter
public class SolrNamedObject extends SolrDatedObject {
    @Indexed(type = FieldType.FOLDING)
    @Field
    protected String name;
}