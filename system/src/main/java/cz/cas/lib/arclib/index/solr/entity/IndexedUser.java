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
public class IndexedUser extends IndexedDatedObject {
    @Field
    @Indexed(type = IndexFieldType.STRING)
    private String username;
    @Field
    @Indexed(type = IndexFieldType.FOLDING)
    private String firstName;
    @Field
    @Indexed(type = IndexFieldType.FOLDING)
    private String lastName;
    @Field
    @Indexed(type = IndexFieldType.TEXT)
    private String email;
    @Field
    @Indexed(type = IndexFieldType.TEXT)
    private String ldapDn;
    @Field
    @Indexed(type = IndexFieldType.STRING)
    private String producerId;
    @Field
    @Indexed(type = IndexFieldType.FOLDING, copyTo = {"producerName_sort"})
    private String producerName;
}
