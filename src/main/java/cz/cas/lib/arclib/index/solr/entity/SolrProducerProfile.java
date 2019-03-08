package cz.cas.lib.arclib.index.solr.entity;

import cz.cas.lib.arclib.domain.HashType;
import cz.cas.lib.core.index.solr.FieldType;
import cz.cas.lib.core.index.solr.SolrNamedObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;


@Getter
@Setter
@SolrDocument(collection = "producerProfile")
public class SolrProducerProfile extends SolrNamedObject {

    @Field
    @Indexed(type = FieldType.STRING)
    private String producerId;

    @Field
    @Indexed(type = FieldType.STRING)
    private String externalId;

    @Field
    @Indexed(type = FieldType.FOLDING)
    private String producerName;

    @Field
    @Indexed(type = FieldType.TEXT)
    private String producerTransferAreaPath;

    @Field
    @Indexed(type = FieldType.STRING)
    private String sipProfileId;

    @Field
    @Indexed(type = FieldType.FOLDING)
    private String sipProfileName;

    @Field
    @Indexed(type = FieldType.STRING)
    private HashType hashType;

    @Field
    @Indexed(type = FieldType.STRING)
    private String workflowDefinitionId;

    @Field
    @Indexed(type = FieldType.FOLDING)
    private String workflowDefinitionName;

    @Field
    @Indexed(type = FieldType.STRING)
    private String validationProfileId;

    @Field
    @Indexed(type = FieldType.FOLDING)
    private String validationProfileName;

    @Field
    @Indexed
    private Boolean debuggingModeActive;
}
