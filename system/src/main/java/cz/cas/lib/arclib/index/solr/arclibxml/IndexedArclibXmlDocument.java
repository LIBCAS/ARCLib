package cz.cas.lib.arclib.index.solr.arclibxml;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.index.solr.IndexQueryUtils;
import cz.cas.lib.arclib.service.tableexport.ExportableTable;
import cz.cas.lib.arclib.service.tableexport.TableDataType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.solr.client.solrj.beans.Field;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
public class IndexedArclibXmlDocument implements Serializable, ExportableTable {

    //names of fields in SOLR required for application logic
    public static final String PRODUCER_ID = "producer_id";
    public static final String PRODUCER_NAME = "producer_name";
    public static final String USER_NAME = "user_name";
    public static final String DEBUG_MODE = "debug_mode";
    public static final String AIP_STATE = "aip_state";
    public static final String MAIN_INDEX_TYPE_VALUE = "arclibXmlMainIndex";

    //these are also stored in ARCLib XML
    public static final String ID = "id";
    public static final String SIP_ID = "sip_id";
    public static final String SIP_VERSION_NUMBER = "sip_version_number";
    public static final String XML_VERSION_NUMBER = "xml_version_number";
    public static final String SIP_VERSION_OF = "sip_version_of";
    public static final String XML_VERSION_OF = "xml_version_of";
    public static final String CREATED = "created";
    public static final String AUTHORIAL_ID = "authorial_id";
    public static final String LATEST = "latest";
    public static final String LATEST_DATA = "latest_data";
    public static final String TYPE = "type";

    //special fields of the element nested collection
    public static final String ELEMENT_INDEX_TYPE_VALUE = "element";
    public static final String ELEMENT_NAME = "element_name";
    public static final String ELEMENT_CONTENT = "element_content";
    public static final String ELEMENT_ATTRIBUTE_NAMES = "element_attribute_names";
    public static final String ELEMENT_ATTRIBUTE_VALUES = "element_attribute_values";

    /**
     * Fields of AIP XML which has to be indexed.
     */
    @Field("*")
    @Getter
    private Map<String, Object> fields = new HashMap<>();

    /**
     * Children elements
     * key is the type of child, see <i>Index Child</i> section of <i>arclibXmlIndexConfig.csv</i>
     * <p>
     * <b>Children are not loaded automatically</b> and their loading is expensive so the field should be loaded
     * and used only when needed.
     * </p>
     */
    @Getter
    private Map<String, List<Map<String, Collection<Object>>>> children = new HashMap<>();

    /**
     * ID of the document, equal to id {@link IngestWorkflow#externalId}.
     * XPath in document: /METS:mets/METS:metsHdr/@ID
     */
    public String getId() {
        return getSingleStringValue(ID);
    }

    //there were some objectmapper problems with this upon deserialization of saved query result
//    /**
//     * Record creation time in ISO UTC format, equal to {@link IngestWorkflow#created}
//     * XPath in document: /METS:mets/METS:metsHdr/@CREATEDATE
//     */
//    public Date getCreated() {
//        return getSingleDateValue(CREATED);
//    }

    /**
     * ID of the Producer.
     * assigned by application
     */
    public String getProducerId() {
        return getSingleStringValue(PRODUCER_ID);
    }

    /**
     * unique name of the Producer.
     * assigned by application
     */
    public String getProducerName() {
        return getSingleStringValue(PRODUCER_NAME);
    }

    /**
     * unique name of the User.
     * assigned by application
     */
    public String getUserName() {
        return getSingleStringValue(USER_NAME);
    }

    /**
     * Authorial id of the authorial package, equal to id {@link AuthorialPackage#authorialId}.
     * XPath in document: /METS:mets/METS:metsHdr/METS:altRecordID[@TYPE='original SIP identifier']
     */
    public String getAuthorialId() {
        return getSingleStringValue(AUTHORIAL_ID);
    }

    /**
     * ID of the SIP.
     * assigned by application
     * XPath in document: /METS:mets/@OBJID
     */
    public String getSipId() {
        return getSingleStringValue(SIP_ID);
    }

    /**
     * SIP version number.
     * assigned by application
     */
    public Integer getSipVersionNumber() {
        return getSingleIntValue(SIP_VERSION_NUMBER);
    }

    /**
     * XML version number.
     * assigned by application
     */
    public Integer getXmlVersionNumber() {
        return getSingleIntValue(XML_VERSION_NUMBER);
    }

    /**
     * Identifier of the previous version of SIP .
     */
    public String getSipVersionOf() {
        return getSingleStringValue(SIP_VERSION_OF);
    }

    /**
     * Identifier of the previous version of XML.
     */
    public String getXmlVersionOf() {
        return getSingleStringValue(XML_VERSION_OF);
    }

    public Boolean getDebugMode() {
        return getSingleBooleanValue(DEBUG_MODE);
    }

    /**
     * State of the AIP at archival storage.
     */
    public IndexedAipState getAipState() {
        return IndexedAipState.valueOf(getSingleStringValue(AIP_STATE));
    }

    public Boolean getLatest() {
        return getSingleBooleanValue(LATEST);
    }

    public Boolean getLatestData() {
        return getSingleBooleanValue(LATEST_DATA);
    }

    public String getIndexType() {
        return getSingleStringValue(IndexQueryUtils.TYPE_FIELD);
    }

    public String getType() {
        return getSingleStringValue(TYPE);
    }

    private String getSingleStringValue(String field) {
        return getSingleValue(field, String.class);
    }

    private Boolean getSingleBooleanValue(String field) {
        return getSingleValue(field, Boolean.class);
    }

    private Integer getSingleIntValue(String field) {
        return getSingleValue(field, Integer.class);
    }

    private Date getSingleDateValue(String field) {
        return getSingleValue(field, Date.class);
    }

    private <T> T getSingleValue(String field, Class<T> clazz) {
        Object value = fields.get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            return clazz.cast(((List<?>) value).get(0));
        }
        return clazz.cast(value);
    }

    @Override
    public Object getExportTableValue(String col) {
        return switch (col) {
            case "updated" -> getSingleDateValue("updated");
            case "label" -> getSingleStringValue("label");
            case AUTHORIAL_ID -> getAuthorialId();
            case SIP_ID -> getSipId();
            case XML_VERSION_NUMBER -> getXmlVersionNumber();
            case AIP_STATE -> getAipState();
            default -> null;
        };
    }

    public static List<TableDataType> getExportTableConfig(List<String> columns) {
        return columns.stream().map(IndexedArclibXmlDocument::getExportTableConfig).collect(Collectors.toList());
    }

    private static TableDataType getExportTableConfig(String col) {
        return switch (col) {
            case SIP_ID -> TableDataType.OTHER;
            case "label", AUTHORIAL_ID, XML_VERSION_NUMBER, AIP_STATE -> TableDataType.STRING_AUTO_SIZE;
            case "updated" -> TableDataType.DATE_TIME;
            default -> throw new UnsupportedOperationException("unsupported export column: " + col);
        };
    }
}