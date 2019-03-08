package cz.cas.lib.core.index.example;

import cz.cas.lib.core.index.solr.SolrDictionaryObject;
import cz.cas.lib.core.index.solr.SolrReference;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

@SolrDocument(collection = "test")
@Getter
@Setter
public class MySolrObject extends SolrDictionaryObject {
    @Field(child = true)
    @Indexed(readonly = true)
    private SolrReference state;
}
