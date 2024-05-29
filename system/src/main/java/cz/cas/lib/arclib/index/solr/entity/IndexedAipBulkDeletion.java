package cz.cas.lib.arclib.index.solr.entity;

import cz.cas.lib.arclib.domain.packages.AipBulkDeletionState;
import cz.cas.lib.core.index.solr.IndexFieldType;
import cz.cas.lib.core.index.solr.IndexedDatedObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

@Getter
@Setter
@SolrDocument(collection = "arclibDomainC")
public class IndexedAipBulkDeletion extends IndexedDatedObject {
    @Field
    @Indexed(type = IndexFieldType.FOLDING, copyTo = {"producerName_sort"})
    private String producerName;

    @Field
    @Indexed(type = IndexFieldType.FOLDING, copyTo = {"userName_sort"})
    private String userName;

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private AipBulkDeletionState state;

    @Field
    @Indexed(type = IndexFieldType.BOOLEAN)
    private boolean deleteIfNewerVersionsDeleted;
}
