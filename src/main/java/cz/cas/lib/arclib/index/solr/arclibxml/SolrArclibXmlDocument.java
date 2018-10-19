package cz.cas.lib.arclib.index.solr.arclibxml;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Dynamic;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

import java.io.Serializable;
import java.util.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SolrDocument
public class SolrArclibXmlDocument implements Serializable {

    /**
     * names of fields in SOLR required for application logic
     */
    public static final String ID = "id";
    public static final String PRODUCER_ID = "producer_id";
    public static final String USER_ID = "user_id";
    public static final String SIP_ID = "sip_id";
    public static final String SIP_VERSION_NUMBER = "sip_version_number";
    public static final String XML_VERSION_NUMBER = "xml_version_number";
    public static final String SIP_VERSION_OF = "sip_version_of";
    public static final String XML_VERSION_OF = "xml_version_of";
    public static final String CREATED = "created";
    public static final String DOCUMENT = "document";
    public static final String STATE = "state";
    public static final String AUTHORIAL_ID = "authorial_id";

    /**
     * ID of the document, equal to id {@link IngestWorkflow#externalId}.
     * XPath in document: /METS:mets/METS:metsHdr/@ID
     */
    @Field(value = ID)
    @Indexed
    private String id;

    /**
     * Record creation time in ISO UTC format, equal to {@link IngestWorkflow#created}
     * XPath in document: /METS:mets/METS:metsHdr/@CREATEDATE
     */
    @Field(value = CREATED)
    @Indexed
    private Date created;

    /**
     * ID of the Producer.
     * assigned by application
     */
    @Field(value = PRODUCER_ID)
    @Indexed
    private String producerId;

    /**
     * ID of the User.
     * assigned by application
     */
    @Field(value = USER_ID)
    @Indexed
    private String userId;

    /**
     * Authorial id of the authorial package, equal to id {@link AuthorialPackage#authorialId}.
     * XPath in document: /METS:mets/METS:metsHdr/METS:altRecordID[@TYPE='original SIP identifier']
     */
    @Field(value = AUTHORIAL_ID)
    @Indexed
    private String authorialId;

    /**
     * ID of the SIP.
     * assigned by application
     * XPath in document: /METS:mets/@OBJID
     */
    @Field(value = SIP_ID)
    @Indexed
    private String sipId;

    /**
     * SIP version number.
     * assigned by application
     */
    @Field(value = SIP_VERSION_NUMBER)
    @Indexed
    private Integer sipVersionNumber;

    /**
     * XML version number.
     * assigned by application
     */
    @Field(value = XML_VERSION_NUMBER)
    @Indexed
    private Integer xmlVersionNumber;

    /**
     * Identifier of the previous version of SIP .
     */
    @Field(value = SIP_VERSION_OF)
    @Indexed
    private String sipVersionOf;

    /**
     * Identifier of the previous version of XML.
     */
    @Field(value = XML_VERSION_OF)
    @Indexed
    private String xmlVersionOf;

    /**
     * The whole document is indexed as fulltext.
     */
    @Field(value = DOCUMENT)
    @Indexed
    private String document;

    /**
     * State of the Ingest workflow.
     */
    @Field(value = STATE)
    @Indexed
    private IndexedArclibXmlDocumentState state;

    /**
     * Other fields of AIP XML which has to be indexed.
     */
    @Field("*")
    @Indexed
    @Dynamic
    private Map<String, Object> fields = new HashMap<>();

    /**
     * Adds field with its value to Solr document. If the field already exists values are stored in list.
     *
     * @param fieldKey
     * @param newFieldValue
     */
    public void addField(String fieldKey, Object newFieldValue) {
        if (fields.containsKey(fieldKey)) {
            Object oldAttrValue = fields.get(fieldKey);
            if (oldAttrValue instanceof Set)
                ((HashSet) oldAttrValue).add(newFieldValue);
            else {
                Set<Object> fieldValues = new HashSet<>();
                fieldValues.add(fields.get(fieldKey));
                fieldValues.add(newFieldValue);
                fields.put(fieldKey, fieldValues);
            }
        } else
            fields.put(fieldKey, newFieldValue);
    }
}