package cz.cas.lib.arclib.index.solr.arclibxml;

import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.index.AipXmlNodeValueType;
import cz.cas.lib.arclib.index.ArclibXmlField;
import cz.cas.lib.arclib.index.ArclibXmlIndexTypeConfig;
import cz.cas.lib.arclib.index.IndexArclibXmlStore;
import cz.cas.lib.arclib.index.solr.IndexQueryUtils;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.AipQueryStore;
import cz.cas.lib.arclib.utils.XmlUtils;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.index.solr.IndexField;
import cz.cas.lib.core.index.solr.IndexFieldType;
import cz.cas.lib.core.store.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.solr.UncategorizedSolrException;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.mapping.Dynamic;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.bind.DatatypeConverter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.index.solr.IndexQueryUtils.*;
import static cz.cas.lib.arclib.utils.ArclibUtils.*;
import static cz.cas.lib.arclib.utils.XmlUtils.createDomAndXpath;
import static cz.cas.lib.core.util.Utils.asSet;
import static org.w3c.dom.Node.TEXT_NODE;

@Service
@Slf4j
public class IndexedArclibXmlStore implements IndexArclibXmlStore<IndexedArclibXmlDocument> {
    /**
     * Configuration of those index fields which are indexed according to arclibXmlDefinition.csv
     * Map contains collection names (main and nested) as keys and their Xpath configurations as values.
     */
    private Map<String, ArclibXmlIndexTypeConfig> arclibXmlXpathIndexConfig = new HashMap<>();

    private SolrTemplate solrTemplate;
    private AipQueryStore aipQueryStore;
    private UserDetails userDetails;
    private String coreName;
    private Map<String, String> uris = new HashMap<>();
    @Getter
    private Resource arclibXmlDefinition;

    @SneakyThrows
    @Override
    public void createIndex(byte[] arclibXml, String producerId, String producerName, String userName, IndexedAipState aipState, boolean debuggingModeActive, boolean latestVersion) {
        Pair<Document, XPath> domAndXpath = createDomAndXpath(new ByteArrayInputStream(arclibXml), uris);
        Document arclibXmlDom = domAndXpath.getKey();
        XPath xpath = domAndXpath.getValue();

        HashMap<String, ArclibXmlIndexTypeConfig> collectionsConfig = new HashMap<>(arclibXmlXpathIndexConfig);

        //add main doc
        ArclibXmlIndexTypeConfig mainDocConfig = collectionsConfig.get(getMainDocumentIndexType());
        SolrInputDocument mainDoc = new SolrInputDocument();
        mainDoc.addField(IndexQueryUtils.TYPE_FIELD, mainDocConfig.getIndexType());
        mainDoc.addField(IndexedArclibXmlDocument.PRODUCER_NAME, producerName);
        mainDoc.addField(IndexedArclibXmlDocument.PRODUCER_ID, producerId);
        mainDoc.addField(IndexedArclibXmlDocument.LATEST, latestVersion);
        if (aipState != null)
            mainDoc.addField(IndexedArclibXmlDocument.AIP_STATE, aipState.toString());
        mainDoc.addField(IndexedArclibXmlDocument.USER_NAME, userName);
        mainDoc.addField(IndexedArclibXmlDocument.DEBUG_MODE, debuggingModeActive);
        for (ArclibXmlField conf : mainDocConfig.getIndexedFieldConfig()) {
            NodeList nodes = (NodeList) xpath.evaluate(conf.getXpath(), arclibXmlDom, XPathConstants.NODESET);
            addFieldToDocument(mainDoc, conf, nodes);
        }
        Object mainDocId = mainDoc.getFieldValue(IndexedArclibXmlDocument.ID);
        if (!(mainDocId instanceof String) || ((String) mainDocId).isEmpty())
            throw new IllegalStateException("ID of document to be indexed was not found in the document");
        String mainDocIdString = (String) mainDocId;
        collectionsConfig.remove(getMainDocumentIndexType());

        //add child docs
        for (String indexType : collectionsConfig.keySet()) {
            ArclibXmlIndexTypeConfig collectionConfig = collectionsConfig.get(indexType);
            NodeList rootNodes = (NodeList) xpath.evaluate(collectionConfig.getRootXpath(), arclibXmlDom, XPathConstants.NODESET);
            if (rootNodes == null || rootNodes.getLength() == 0)
                continue;
            for (int i = 0; i < rootNodes.getLength(); i++) {
                Node rootNode = rootNodes.item(i);

                SolrInputDocument doc = new SolrInputDocument();
                doc.addField(IndexQueryUtils.TYPE_FIELD, collectionConfig.getIndexType());
                for (ArclibXmlField conf : collectionConfig.getIndexedFieldConfig()) {
                    NodeList nodes = (NodeList) xpath.evaluate(conf.getXpath(), rootNode, XPathConstants.NODESET);
                    addFieldToDocument(doc, conf, nodes);
                }
                doc.addField(IndexedArclibXmlDocument.ID, mainDocIdString + "_" + indexType + "_" + i);
                mainDoc.addChildDocument(doc);
            }
        }

        List<ElementsToIndex> elementsToIndex = parseElementsToIndex(arclibXmlDom);
        for (int i = 0; i < elementsToIndex.size(); i++) {
            ElementsToIndex e = elementsToIndex.get(i);
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField(IndexQueryUtils.TYPE_FIELD, IndexedArclibXmlDocument.ELEMENT_INDEX_TYPE_VALUE);
            doc.addField(IndexedArclibXmlDocument.ELEMENT_NAME, e.getElementName());
            doc.addField(IndexedArclibXmlDocument.ELEMENT_CONTENT, e.getTextContent());
            for (Pair<String, String> attribute : e.getAttributes()) {
                doc.addField(IndexedArclibXmlDocument.ELEMENT_ATTRIBUTE_NAMES, attribute.getKey());
                String value = attribute.getValue();
                if (value != null && !value.isBlank())
                    doc.addField(IndexedArclibXmlDocument.ELEMENT_ATTRIBUTE_VALUES, value.trim());
            }
            doc.addField(IndexedArclibXmlDocument.ID, mainDocIdString + "_" + IndexedArclibXmlDocument.ELEMENT_INDEX_TYPE_VALUE + "_" + i);
            mainDoc.addChildDocument(doc);
        }

        try {
            removeIndex(mainDocIdString);
            solrTemplate.saveDocument(coreName, mainDoc);
            solrTemplate.commit(coreName);
        } catch (UncategorizedSolrException ex) {
            log.error(ex.getMessage());
            throw ex;
        }
    }

    @Override
    @Transactional
    public Result<IndexedArclibXmlDocument> findAll(Params params, String queryName) {
        SimpleQuery query = new SimpleQuery();
        Map<String, IndexField> indexedFields = INDEXED_FIELDS_MAP.get(getMainDocumentIndexType());
        initializeQuery(query, params, indexedFields);

        query.addCriteria(Criteria.where(IndexQueryUtils.TYPE_FIELD).in(getMainDocumentIndexType()));
        if (params.getInternalQuery() != null)
            query.addCriteria(params.getInternalQuery());
        query.addFilterQuery(new SimpleFilterQuery(buildFilters(params, getMainDocumentIndexType(), indexedFields)));
        log.info("Searching for documents");
        Page<IndexedArclibXmlDocument> page;
        try {
            page = solrTemplate.query(coreName, query, IndexedArclibXmlDocument.class);
        } catch (UncategorizedSolrException ex) {
            Matcher matcher = Pattern.compile(".+ undefined field (.+)").matcher(ex.getMessage());
            if (matcher.find()) {
                String msg = "query contains undefined field: " + matcher.group(1);
                throw new BadArgument(msg);
            }
            throw ex;
        } catch (DataAccessResourceFailureException ex) {
            throw new BadArgument(ex.getMessage());
        }
        Result<IndexedArclibXmlDocument> result = new Result<>();
        result.setItems(page.getContent());
        result.setCount(page.getTotalElements());
        if (queryName != null && !queryName.trim().isEmpty())
            aipQueryStore.save(new AipQuery(new User(userDetails.getId()), result, params, queryName));
        log.info("Found documents: " + Arrays.toString(result.getItems().stream().map(IndexedArclibXmlDocument::getId).toArray()));
        return result;
    }


    /**
     * changes state of the document.. this can't be done via partial update as it breaks parent-children relationship
     *
     * @param arclibXmlDocumentId
     * @param newAipState
     */
    @Override
    public void changeAipState(String arclibXmlDocumentId, IndexedAipState newAipState, byte[] aipXml) {
        Map<String, Object> aclibXmlIndexDocument = findArclibXmlIndexDocument(arclibXmlDocumentId);
        String producerId = (String) ((ArrayList) aclibXmlIndexDocument.get(IndexedArclibXmlDocument.PRODUCER_ID)).get(0);
        String responsiblePerson = (String) ((ArrayList) aclibXmlIndexDocument.get(IndexedArclibXmlDocument.USER_NAME)).get(0);
        String producer_name = (String) ((ArrayList) aclibXmlIndexDocument.get(IndexedArclibXmlDocument.PRODUCER_NAME)).get(0);
        boolean debugMode = (Boolean) ((ArrayList) aclibXmlIndexDocument.get(IndexedArclibXmlDocument.DEBUG_MODE)).get(0);
        boolean latestVersion = (Boolean) ((ArrayList) aclibXmlIndexDocument.get(IndexedArclibXmlDocument.LATEST)).get(0);
        createIndex(aipXml,
                producerId,
                producer_name,
                responsiblePerson,
                newAipState,
                debugMode,
                latestVersion);
    }

    /**
     * changes {@link IndexedArclibXmlDocument#LATEST} flag of the document.. this can't be done via partial update as it breaks parent-children relationship
     *
     * @param arclibXmlDocumentId
     * @param flag
     */
    @Override
    public void setLatestFlag(String arclibXmlDocumentId, boolean flag, byte[] aipXml) {
        Map<String, Object> aclibXmlIndexDocument = findArclibXmlIndexDocument(arclibXmlDocumentId);
        IndexedAipState aipState = IndexedAipState.valueOf((String) ((ArrayList) aclibXmlIndexDocument.get(IndexedArclibXmlDocument.AIP_STATE)).get(0));
        String producerId = (String) ((ArrayList) aclibXmlIndexDocument.get(IndexedArclibXmlDocument.PRODUCER_ID)).get(0);
        String responsiblePerson = (String) ((ArrayList) aclibXmlIndexDocument.get(IndexedArclibXmlDocument.USER_NAME)).get(0);
        String producer_name = (String) ((ArrayList) aclibXmlIndexDocument.get(IndexedArclibXmlDocument.PRODUCER_NAME)).get(0);
        boolean debugMode = (Boolean) ((ArrayList) aclibXmlIndexDocument.get(IndexedArclibXmlDocument.DEBUG_MODE)).get(0);
        createIndex(aipXml,
                producerId,
                producer_name,
                responsiblePerson,
                aipState,
                debugMode,
                flag);
    }


    @Override
    public void removeIndex(String id) {
        SolrDataQuery deleteQuery = new SimpleQuery("id:" + id + "*");
        solrTemplate.delete(coreName, deleteQuery);
        solrTemplate.commit(coreName);
    }

    @Override
    public String getMainDocumentIndexType() {
        return IndexedArclibXmlDocument.MAIN_INDEX_TYPE_VALUE;
    }

    /**
     * Finds ArclibXml index document by the external id and returns all the indexed attributes of the main document, OMMITING CHILDREN
     *
     * @param externalId external id of the ArclibXml index document
     * @return map of indexed attributes and their values
     */
    public Map<String, Object> findArclibXmlIndexDocument(String externalId) {
        SimpleQuery query = new SimpleQuery();
        query.addCriteria(Criteria.where(IndexedArclibXmlDocument.ID).in(asSet(externalId)));
        log.info("Searching for document with id " + externalId);
        Page<IndexedArclibXmlDocument> page = solrTemplate.query(coreName, query, IndexedArclibXmlDocument.class);
        if (page.getContent().size() == 0) return null;
        return page.getContent().get(0).getFields();
    }

    /**
     * Fills {@link #arclibXmlXpathIndexConfig} (used during indexing) with field definitions from CSV config and
     * {@link IndexQueryUtils#INDEXED_FIELDS_MAP} (used during every ARCLib XML query) with field definitions from
     * CSV config, annotations from {@link IndexedArclibXmlDocument} and with few other special fields.
     * <p>
     * If the field (e.g. {@link IndexedArclibXmlDocument#AUTHORIAL_ID})
     * is defined in both, CSV and Java class, the definition from CSV is taken
     * </p>
     *
     */
    @PostConstruct
    @SneakyThrows
    public void init() {
        //prepare
        Pair<Document, XPath> domAndXpath;
        try (InputStream is = getClass().getResourceAsStream("/index/config/schema.xml")) {
            domAndXpath = createDomAndXpath(is, null);
        }
        Document schemaXmlDom = domAndXpath.getKey();
        XPath xPath = domAndXpath.getValue();

        Set<String> supportedFieldTypes = Arrays.stream(IndexFieldType.class.getDeclaredFields()).map(f -> {
            try {
                return (String) FieldUtils.readStaticField(f);
            } catch (IllegalAccessException e) {
                throw new GeneralException("could not read constant value of field: " + f);
            }
        }).collect(Collectors.toSet());

        //declare main collection
        ArclibXmlIndexTypeConfig mainCollection = new ArclibXmlIndexTypeConfig(null, getMainDocumentIndexType());
        arclibXmlXpathIndexConfig = new HashMap<>();
        arclibXmlXpathIndexConfig.put(mainCollection.getIndexType(), mainCollection);

        //fill INDEXED_FIELDS_MAP with all fields defined in CSV
        parseCsvConfig(mainCollection, arclibXmlXpathIndexConfig);
        arclibXmlXpathIndexConfig.values().forEach(c -> addTypeConfigToIndexedFieldsMap(schemaXmlDom, xPath, supportedFieldTypes, c));

        //fill INDEXED_FIELDS_MAP with those fields which are not configured in CSV but are declared via annotations
        Map<String, IndexField> parentIndexFields = INDEXED_FIELDS_MAP.get(IndexedArclibXmlDocument.MAIN_INDEX_TYPE_VALUE);
        for (java.lang.reflect.Field field : FieldUtils.getFieldsWithAnnotation(IndexedArclibXmlDocument.class, Indexed.class)) {
            if (field.isAnnotationPresent(Dynamic.class))
                continue;
            IndexField solrField = new IndexField(field);
            if (parentIndexFields.containsKey(solrField.getFieldName()))
                continue;
            parentIndexFields.put(solrField.getFieldName(), solrField);
        }

        //fill INDEXED_FIELDS_MAP with special nested "element" collection
        ArclibXmlIndexTypeConfig elementCollectionConfig = new ArclibXmlIndexTypeConfig(null, IndexedArclibXmlDocument.ELEMENT_INDEX_TYPE_VALUE);
        elementCollectionConfig.setIndexedFieldConfig(Set.of(
                new ArclibXmlField(IndexedArclibXmlDocument.ELEMENT_NAME, AipXmlNodeValueType.OTHER, null, true),
                new ArclibXmlField(IndexedArclibXmlDocument.ELEMENT_CONTENT, AipXmlNodeValueType.OTHER, null, true),
                new ArclibXmlField(IndexedArclibXmlDocument.ELEMENT_ATTRIBUTE_NAMES, AipXmlNodeValueType.OTHER, null, true),
                new ArclibXmlField(IndexedArclibXmlDocument.ELEMENT_ATTRIBUTE_VALUES, AipXmlNodeValueType.OTHER, null, true)
        ));
        addTypeConfigToIndexedFieldsMap(schemaXmlDom, xPath, supportedFieldTypes, elementCollectionConfig);
    }

    private void addFieldToDocument(SolrInputDocument doc, ArclibXmlField conf, NodeList nodes) {
        if (nodes == null || nodes.getLength() == 0)
            return;
        for (int i = 0; i < nodes.getLength(); i++) {
            Calendar parsedDate;
            try {
                switch (conf.getAipXmlNodeValueType()) {
                    case DATETIME:
                        parsedDate = DatatypeConverter.parseDateTime(nodes.item(i).getTextContent());
                        String parsedDateTimeString = DateTimeFormatter.ISO_INSTANT.format(parsedDate.toInstant().atOffset(ZoneOffset.UTC));
                        doc.addField(conf.getFieldName(), parsedDateTimeString);
                        break;
                    case DATE:
                        parsedDate = DatatypeConverter.parseDate(nodes.item(i).getTextContent());
                        String parsedDateString = DateTimeFormatter.ISO_INSTANT.format(parsedDate.toInstant().atOffset(ZoneOffset.UTC));
                        doc.addField(conf.getFieldName(), parsedDateString);
                        break;
                    case TIME:
                        ZonedDateTime time = DatatypeConverter.parseTime(nodes.item(i).getTextContent()).toInstant().atZone(ZoneId.systemDefault());
                        long parsedTime = time.getHour() * 60 * 60 * 1000 + time.getMinute() * 60 * 1000 + time.getSecond() * 1000;
                        doc.addField(conf.getFieldName(), parsedTime);
                        break;
                    default:
                        if (conf.isSimple()) {
                            doc.addField(conf.getFieldName(), nodes.item(i).getTextContent());
                        } else {
                            doc.addField(conf.getFieldName(), XmlUtils.extractTextFromAllElements(new StringBuilder(), nodes.item(i)).toString());
                        }
                }
            } catch (IllegalArgumentException | NullPointerException ex) {
                String msg = String.format("Could not parse %s as %s", nodes.item(i).getTextContent(), conf.getAipXmlNodeValueType());
                throw new BadArgument(msg);
            }
        }
    }

    /**
     * Parses all fields of the passed indexTypeConfig and fills them within the global variable {@link IndexQueryUtils#INDEXED_FIELDS_MAP}
     *
     * @param schemaXml           schema.xml
     * @param supportedFieldTypes all field types supported by the system
     * @param indexTypeConfig     index type config
     */
    private void addTypeConfigToIndexedFieldsMap(Document schemaXml, XPath xPath, Set<String> supportedFieldTypes, ArclibXmlIndexTypeConfig indexTypeConfig) {
        Map<String, IndexField> collectionConfig = new HashMap<>();
        for (ArclibXmlField field : indexTypeConfig.getIndexedFieldConfig()) {
            IndexField indexField = transformConfig(schemaXml, xPath, field);
            if (!supportedFieldTypes.contains(indexField.getFieldType()))
                throw new GeneralException("unsupported field type: " + indexField.getFieldType() + " of field: " + indexField.getFieldName() + " found in Arclib XML Solr schema");
            collectionConfig.put(indexField.getFieldName(), indexField);
        }
        INDEXED_FIELDS_MAP.put(indexTypeConfig.getIndexType(), collectionConfig);
    }

    /**
     * Searches schema.xml for particular field and transforms the passed config object into {@link IndexField} object
     *
     * @param schemaXml   SOLR schema
     * @param fieldConfig config of single field
     */
    private IndexField transformConfig(Document schemaXml, XPath xPath, ArclibXmlField fieldConfig) {
        String fieldName = fieldConfig.getFieldName();
        String fieldType;
        String sortField = null;
        String eqField = null;
        try {
            Node fieldTypeNode = (Node) xPath.evaluate("//field[@name='" + fieldName + "']/@type", schemaXml, XPathConstants.NODE);
            fieldType = fieldTypeNode.getTextContent();
            NodeList copyFields = (NodeList) xPath.evaluate("//copyField[@source='" + fieldName + "']/@dest", schemaXml, XPathConstants.NODESET);
            for (int i = 0; i < copyFields.getLength(); i++) {
                String cpyFieldDest = copyFields.item(i).getTextContent();
                if (cpyFieldDest.endsWith(IndexField.STRING_SUFFIX))
                    eqField = cpyFieldDest;
                if (cpyFieldDest.endsWith(IndexField.SORT_SUFFIX))
                    sortField = cpyFieldDest;
            }
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        return new IndexField(fieldName, fieldType, sortField, eqField);
    }

    private List<ElementsToIndex> parseElementsToIndex(Document dom) {
        List<ElementsToIndex> result = new ArrayList<>();
        NodeList elements = dom.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            Element e = (Element) elements.item(i);

            List<Pair<String, String>> parsedAttributes = new ArrayList<>();
            NamedNodeMap attributes = e.getAttributes();
            for (int j = 0; j < attributes.getLength(); j++) {
                Attr attr = (Attr) attributes.item(j);
                parsedAttributes.add(Pair.of(attr.getLocalName(), attr.getValue()));
            }

            StringBuilder textContentBuilder = new StringBuilder();
            NodeList childNodes = e.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                Node c = childNodes.item(j);
                if (c.getNodeType() == TEXT_NODE)
                    textContentBuilder.append(c.getTextContent().trim()).append(" ");
            }
            String textContentString = textContentBuilder.toString().trim();

            if (parsedAttributes.isEmpty() && textContentString.isEmpty())
                continue;

            result.add(new ElementsToIndex(e.getLocalName(), textContentString.isEmpty() ? null : textContentString, parsedAttributes));
        }
        return result;
    }

    @Getter
    @AllArgsConstructor
    private static final class ElementsToIndex {
        /**
         * unqualified element name
         */
        private String elementName;
        /**
         * text content of the attribute, or null if empty
         */
        private String textContent;
        /**
         * list of pairs of unqualified attribute name and its value.. list may be empty
         */
        private List<Pair<String, String>> attributes;
    }

    @Inject
    public void setAipQueryStore(AipQueryStore aipQueryStore) {
        this.aipQueryStore = aipQueryStore;
    }

    @Autowired
    @Qualifier("ArclibXmlSolrTemplate")
    public void setSolrTemplate(SolrTemplate solrTemplate) {
        this.solrTemplate = solrTemplate;
    }

    @Inject
    public void setCoreName(@Value("${solr.arclibxml.corename}") String coreName) {
        this.coreName = coreName;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Inject
    public void setArclibXmlDefinition(@Value("${arclib.arclibXmlDefinition}")
                                               Resource arclibXmlDefinition) {
        this.arclibXmlDefinition = arclibXmlDefinition;
    }

    @Inject
    public void setUris(@Value("${namespaces.mets}") String mets, @Value("${namespaces.xsi}") String
            xsi, @Value("${namespaces.arclib}") String arclib, @Value("${namespaces" +
            ".premis}") String premis, @Value("${namespaces.oai_dc}") String
                                oai_dc, @Value("${namespaces.dc}") String dc, @Value("${namespaces.xlink}") String xlink) {
        Map<String, String> uris = new HashMap<>();
        uris.put(METS, mets);
        uris.put(ARCLIB, arclib);
        uris.put(PREMIS, premis);
        uris.put(XSI, xsi);
        uris.put(OAIS_DC, oai_dc);
        uris.put(DC, dc);
        uris.put(XLINK,xlink);

        this.uris = uris;
    }
}
