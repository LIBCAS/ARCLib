package cz.cas.lib.core.index.solr;

import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;

@Getter
@Setter
public class IndexedNamedObject extends IndexedDatedObject {
    @Indexed(type = IndexFieldType.FOLDING, copyTo = {"name_sort"})
    @Field
    protected String name;
}