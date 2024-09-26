package cz.cas.lib.core.index.solr;

import cz.cas.lib.core.index.Indexed;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;

@Getter
@Setter
public class IndexedNamedObject extends IndexedDatedObject {
    @Indexed(type = IndexFieldType.FOLDING, copyTo = {"name_sort"})
    @Field
    protected String name;
}