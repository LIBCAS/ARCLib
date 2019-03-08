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
@SolrDocument(collection = "user")
public class SolrUser extends SolrDatedObject {
    @Field
    @Indexed(type = FieldType.STRING)
    private String username;
    @Field
    @Indexed(type = FieldType.FOLDING)
    private String firstName;
    @Field
    @Indexed(type = FieldType.FOLDING)
    private String lastName;
    @Field
    @Indexed(type = FieldType.TEXT)
    private String email;
    @Field
    @Indexed(type = FieldType.TEXT)
    private String ldapDn;
    @Field
    @Indexed(type = FieldType.STRING)
    private String producerId;
    @Field
    @Indexed(type = FieldType.FOLDING)
    private String producerName;
}
