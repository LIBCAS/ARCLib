package cz.inqool.uas.index.solr;

import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

@SolrDocument(solrCoreName = "gettingstarted")
@Getter
@Setter
public class MySolrObject extends SolrDictionaryObject {
    @Field(child = true)
    @Indexed(readonly = true)
    private SolrReference state;
}
