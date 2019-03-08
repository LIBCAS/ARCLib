package cz.cas.lib.arclib.index.solr.arclibxml;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.data.solr.core.mapping.Dynamic;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@Getter
@Setter
@SolrDocument
public class SolrArclibXmlDocument implements Serializable {

    /**
     * names of fields in SOLR required for application logic
     */
    public static final String PRODUCER_ID = "producer_id";
    public static final String PRODUCER_NAME = "producer_name";
    public static final String USER_NAME = "user_name";
    public static final String DOCUMENT = "document";
    public static final String DEBUG_MODE = "debug_mode";
    public static final String STATE = "state";
    public static final String COLLECTION_NAME = "collection_name";

    /**
     * these are also stored in ARCLib XML
     */
    public static final String ID = "id";
    public static final String SIP_ID = "sip_id";
    public static final String SIP_VERSION_NUMBER = "sip_version_number";
    public static final String XML_VERSION_NUMBER = "xml_version_number";
    public static final String SIP_VERSION_OF = "sip_version_of";
    public static final String XML_VERSION_OF = "xml_version_of";
    public static final String CREATED = "created";
    public static final String AUTHORIAL_ID = "authorial_id";
    public static final String DUBLIN_CORE = "dublin_core";

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
     * unique name of the Producer.
     * assigned by application
     */
    @Field(value = PRODUCER_NAME)
    @Indexed
    private String producerName;

    /**
     * unique name of the User.
     * assigned by application
     */
    @Field(value = USER_NAME)
    @Indexed
    private String userName;

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

    @Field(value = DEBUG_MODE)
    @Indexed(defaultValue = "false")
    private Boolean debugMode;

    @Field(value = COLLECTION_NAME)
    @Indexed
    private String collectionName;

    /**
     * Other fields of AIP XML which has to be indexed.
     */
    @Field("*")
    @Indexed
    @Dynamic
    private Map<String, Object> fields = new HashMap<>();

    public static SolrInputDocument createDocument(String id, Date created, String producerId, String producerName, String userName, String authorialId, String sipId, Integer sipVersionNumber, Integer xmlVersionNumber, String sipVersionOf, String xmlVersionOf, String document, IndexedArclibXmlDocumentState state, Boolean debugMode, String collectionName, Map<String, Object> fields) {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField(ID, id);
        doc.addField(CREATED, created);
        doc.addField(PRODUCER_ID, producerId);
        doc.addField(PRODUCER_NAME, producerName);
        doc.addField(USER_NAME, userName);
        doc.addField(AUTHORIAL_ID, authorialId);
        doc.addField(SIP_ID, sipId);
        doc.addField(SIP_VERSION_NUMBER, sipVersionNumber);
        doc.addField(XML_VERSION_NUMBER, xmlVersionNumber);
        doc.addField(SIP_VERSION_OF, sipVersionOf);
        doc.addField(XML_VERSION_OF, xmlVersionOf);
        doc.addField(DOCUMENT, document);
        doc.addField(STATE, state.toString());
        doc.addField(DEBUG_MODE, debugMode);
        doc.addField(COLLECTION_NAME, collectionName);
        return doc;
    }

//    /**
//     * Adds field with its value to Solr document. If the field already exists values are stored in list.
//     *
//     * @param fieldKey
//     * @param newFieldValue
//     */
//    public void addField(SolrInputDocument solrInputDocument, String fieldKey, Object newFieldValue) {
//        if (solrInputDocument.getFieldNames().contains(fieldKey)) {
//            Object oldAttrValue = solrInputDocument.get(fieldKey);
//            if (oldAttrValue instanceof Set)
//                ((HashSet) oldAttrValue).add(newFieldValue);
//            else {
//                Set<Object> fieldValues = new HashSet<>();
//                fieldValues.add(solrInputDocument.get(fieldKey));
//                fieldValues.add(newFieldValue);
//                solr
//                solrInputDocument.put(fieldKey, fieldValues);
//            }
//        } else
//            fields.put(fieldKey, newFieldValue);
//    }
}