package cz.cas.lib.core.index;

import cz.cas.lib.core.index.solr.FieldType;
import cz.cas.lib.core.index.solr.SolrDatedObject;
import cz.cas.lib.core.index.solr.SolrReference;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;


@Getter
@Setter
@SolrDocument(collection = "test")
public class SolrTestEntity extends SolrDatedObject {

    @Field
    @Indexed(type = FieldType.STRING)
    protected String stringAttribute;

    @Field
    @Indexed(type = FieldType.TEXT)
    protected String textAttribute;

    @Field
    @Indexed(type = FieldType.FOLDING)
    protected String foldingAttribute;

    @Field
    @Indexed(type = FieldType.INT)
    private Integer intAttribute;

    @Field
    @Indexed(type = FieldType.DOUBLE)
    private Double doubleAttribute;

    @Field
    @Indexed(type = FieldType.DATE)
    private String localDateAttribute;

    @Field
    @Indexed(type = FieldType.DATE)
    private String instantAttribute;

    @Field(child = true)
    @Indexed(readonly = true)
    private SolrReference dependent;

//    @Field//(child = true)
//    @Indexed(readonly = true)
//    private Set<SolrReference> dependents;
}
