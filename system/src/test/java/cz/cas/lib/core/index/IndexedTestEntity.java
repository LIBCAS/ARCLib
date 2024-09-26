package cz.cas.lib.core.index;

import cz.cas.lib.core.index.solr.IndexField;
import cz.cas.lib.core.index.solr.IndexFieldType;
import cz.cas.lib.core.index.solr.IndexedDatedObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;


@Getter
@Setter
@SolrDocument(collection = "testC")
public class IndexedTestEntity extends IndexedDatedObject {

    @Field
    @Indexed(type = IndexFieldType.STRING, copyTo = {"sortableStringAttribute" + IndexField.SORT_SUFFIX})
    protected String customSortStringAttribute;

    @Field
    @Indexed(type = IndexFieldType.STRING)
    protected String stringAttribute;

    @Field
    @Indexed(type = IndexFieldType.TEXT)
    protected String textAttribute;

    @Field
    @Indexed(type = IndexFieldType.TEXT, copyTo = {"textAttributeWithStringCpyField" + IndexField.STRING_SUFFIX})
    protected String textAttributeWithStringCpyField;

    @Field
    @Indexed(type = IndexFieldType.FOLDING)
    protected String foldingAttribute;

    @Field
    @Indexed(type = IndexFieldType.INT)
    private Integer intAttribute;

    @Field
    @Indexed(type = IndexFieldType.DOUBLE)
    private Double doubleAttribute;

    @Field
    @Indexed(type = IndexFieldType.DATE)
    private String localDateAttribute;

    @Field
    @Indexed(type = IndexFieldType.DATE)
    private String instantAttribute;
}
