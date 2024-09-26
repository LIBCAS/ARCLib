package cz.cas.lib.arclib.index.solr.entity;

import cz.cas.lib.arclib.domain.HashType;
import cz.cas.lib.core.index.Indexed;
import cz.cas.lib.core.index.SolrDocument;
import cz.cas.lib.core.index.solr.IndexFieldType;
import cz.cas.lib.core.index.solr.IndexedNamedObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;


@Getter
@Setter
@SolrDocument(collection = "arclibDomainC")
public class IndexedProducerProfile extends IndexedNamedObject {

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private String producerId;

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private String externalId;

    @Field
    @Indexed(type = IndexFieldType.FOLDING, copyTo = {"producerName_sort"})
    private String producerName;

    @Field
    @Indexed(type = IndexFieldType.TEXT)
    private String producerTransferAreaPath;

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private String sipProfileId;

    @Field
    @Indexed(type = IndexFieldType.FOLDING, copyTo = {"sipProfileName_sort"})
    private String sipProfileName;

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private HashType hashType;

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private String workflowDefinitionId;

    @Field
    @Indexed(type = IndexFieldType.FOLDING, copyTo = {"workflowDefinitionName_sort"})
    private String workflowDefinitionName;

    @Field
    @Indexed(type = IndexFieldType.STRING)
    private String validationProfileId;

    @Field
    @Indexed(type = IndexFieldType.FOLDING, copyTo = {"validationProfileName_sort"})
    private String validationProfileName;

    @Field
    @Indexed(type = IndexFieldType.BOOLEAN)
    private Boolean debuggingModeActive;
}
