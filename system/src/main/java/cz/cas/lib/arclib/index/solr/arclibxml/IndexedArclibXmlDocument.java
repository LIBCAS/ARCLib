package cz.cas.lib.arclib.index.solr.arclibxml;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.index.solr.IndexQueryUtils;
import cz.cas.lib.core.index.solr.IndexFieldType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Dynamic;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static cz.cas.lib.core.index.solr.IndexField.SORT_SUFFIX;
import static cz.cas.lib.core.index.solr.IndexField.STRING_SUFFIX;

@NoArgsConstructor
@Getter
@Setter
@SolrDocument
public class IndexedArclibXmlDocument implements Serializable {

    /**
     * names of fields in SOLR required for application logic
     */
    public static final String PRODUCER_ID = "producer_id";
    public static final String PRODUCER_NAME = "producer_name";
    public static final String USER_NAME = "user_name";
    public static final String CONTENT = "document";
    public static final String DEBUG_MODE = "debug_mode";
    public static final String AIP_STATE = "aip_state";
    public static final String MAIN_INDEX_TYPE_VALUE = "arclibXmlMainIndex";

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
    public static final String LATEST = "latest";

    /**
     * ID of the document, equal to id {@link IngestWorkflow#externalId}.
     * XPath in document: /METS:mets/METS:metsHdr/@ID
     */
    @Field(value = ID)
    @Indexed(type = IndexFieldType.STRING)
    private String id;

    /**
     * Record creation time in ISO UTC format, equal to {@link IngestWorkflow#created}
     * XPath in document: /METS:mets/METS:metsHdr/@CREATEDATE
     */
    @Field(value = CREATED)
    @Indexed(type = IndexFieldType.DATE)
    private Date created;

    /**
     * ID of the Producer.
     * assigned by application
     */
    @Field(value = PRODUCER_ID)
    @Indexed(type = IndexFieldType.STRING)
    private String producerId;

    /**
     * unique name of the Producer.
     * assigned by application
     */
    @Field(value = PRODUCER_NAME)
    @Indexed(type = IndexFieldType.STRING, copyTo = {PRODUCER_NAME + SORT_SUFFIX})
    private String producerName;

    /**
     * unique name of the User.
     * assigned by application
     */
    @Field(value = USER_NAME)
    @Indexed(type = IndexFieldType.STRING, copyTo = {USER_NAME + SORT_SUFFIX})
    private String userName;

    /**
     * Authorial id of the authorial package, equal to id {@link AuthorialPackage#authorialId}.
     * XPath in document: /METS:mets/METS:metsHdr/METS:altRecordID[@TYPE='original SIP identifier']
     */
    @Field(value = AUTHORIAL_ID)
    @Indexed(type = IndexFieldType.FOLDING, copyTo = {AUTHORIAL_ID + SORT_SUFFIX, AUTHORIAL_ID + STRING_SUFFIX})
    private String authorialId;

    /**
     * ID of the SIP.
     * assigned by application
     * XPath in document: /METS:mets/@OBJID
     */
    @Field(value = SIP_ID)
    @Indexed(type = IndexFieldType.STRING)
    private String sipId;

    /**
     * SIP version number.
     * assigned by application
     */
    @Field(value = SIP_VERSION_NUMBER)
    @Indexed(type = IndexFieldType.INT)
    private Integer sipVersionNumber;

    /**
     * XML version number.
     * assigned by application
     */
    @Field(value = XML_VERSION_NUMBER)
    @Indexed(type = IndexFieldType.INT)
    private Integer xmlVersionNumber;

    /**
     * Identifier of the previous version of SIP .
     */
    @Field(value = SIP_VERSION_OF)
    @Indexed(type = IndexFieldType.STRING)
    private String sipVersionOf;

    /**
     * Identifier of the previous version of XML.
     */
    @Field(value = XML_VERSION_OF)
    @Indexed(type = IndexFieldType.STRING)
    private String xmlVersionOf;

    /**
     * concatenation of all text content of elements
     */
    @Field(value = CONTENT)
    @Indexed(type = IndexFieldType.TEXT)
    private String content;

    /**
     * State of the AIP at archival storage.
     */
    @Field(value = AIP_STATE)
    @Indexed(type = IndexFieldType.STRING)
    private IndexedAipState aipState;

    @Field(value = LATEST)
    @Indexed(type = IndexFieldType.BOOLEAN)
    private Boolean latest;

    @Field(value = DEBUG_MODE)
    @Indexed(defaultValue = "false", type = IndexFieldType.BOOLEAN)
    private Boolean debugMode;

    @Field(value = IndexQueryUtils.TYPE_FIELD)
    @Indexed(type = IndexFieldType.STRING)
    private String indexType;

    /**
     * Other fields of AIP XML which has to be indexed.
     */
    @Field("*")
    @Indexed
    @Dynamic
    private Map<String, Object> fields = new HashMap<>();

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