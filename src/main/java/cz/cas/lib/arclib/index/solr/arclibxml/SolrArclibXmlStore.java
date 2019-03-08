package cz.cas.lib.arclib.index.solr.arclibxml;

import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.index.IndexArclibXmlStore;
import cz.cas.lib.arclib.index.IndexCollectionConfig;
import cz.cas.lib.arclib.index.IndexFieldConfig;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.AipQueryStore;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.index.dto.*;
import cz.cas.lib.core.index.solr.util.NestedCriteria;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.solr.UncategorizedSolrException;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.index.solr.SolrQueryUtils.*;
import static cz.cas.lib.arclib.utils.ArclibUtils.*;
import static cz.cas.lib.arclib.utils.XmlUtils.nodeToString;
import static cz.cas.lib.core.util.Utils.asSet;

@Service
@Slf4j
public class SolrArclibXmlStore implements IndexArclibXmlStore<SolrArclibXmlDocument> {

    private SolrTemplate solrTemplate;
    private AipQueryStore aipQueryStore;
    private UserDetails userDetails;
    private String coreName;
    private Map<String, String> uris = new HashMap<>();

    @SneakyThrows
    public void createIndex(String arclibXml, Producer producer, User user, IndexedArclibXmlDocumentState state, boolean debuggingModeActive) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document xml;
        try {
            xml = factory.newDocumentBuilder().parse(new ByteArrayInputStream(arclibXml.getBytes()));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("Error during parsing of XML document");
            throw e;
        }
        XPath xpath = getXpathWithNamespaceContext();
        Map<String, IndexCollectionConfig> collectionsConfig = getArclibXmlCollectionsConfig();

        //add main doc
        IndexCollectionConfig mainDocConfig = collectionsConfig.get(getMainDocumentCollectionName());
        SolrInputDocument mainDoc = new SolrInputDocument();
        mainDoc.addField(SolrArclibXmlDocument.COLLECTION_NAME, mainDocConfig.getCollectionName());
        mainDoc.addField(SolrArclibXmlDocument.PRODUCER_NAME, producer.getName());
        mainDoc.addField(SolrArclibXmlDocument.PRODUCER_ID, producer.getId());
        mainDoc.addField(SolrArclibXmlDocument.USER_NAME, user.getUsername());
        mainDoc.addField(SolrArclibXmlDocument.DOCUMENT, arclibXml);
        mainDoc.addField(SolrArclibXmlDocument.STATE, state.toString());
        mainDoc.addField(SolrArclibXmlDocument.DEBUG_MODE, debuggingModeActive);
        for (IndexFieldConfig conf : mainDocConfig.getIndexedFieldConfig()) {
            NodeList nodes = (NodeList) xpath.evaluate(conf.getXpath(), xml, XPathConstants.NODESET);
            if (conf.getFieldName().equals(SolrArclibXmlDocument.DUBLIN_CORE)) {
                for (int i = 0; i < nodes.getLength(); i++) {
                    String dmdSecId = nodes.item(0).getParentNode().getParentNode().getParentNode().getAttributes().getNamedItem("ID").getTextContent();
                    mainDoc.addField(SolrArclibXmlDocument.DUBLIN_CORE, dmdSecId + " " + nodeToString(nodes.item(i)));
                }
            } else
                addFieldToDocument(mainDoc, conf, nodes);
        }
        Object mainDocId = mainDoc.getFieldValue(SolrArclibXmlDocument.ID);
        if (!(mainDocId instanceof String) || ((String) mainDocId).isEmpty())
            throw new IllegalStateException("ID of document to be indexed was not found in the document");
        String mainDocIdString = (String) mainDocId;
        collectionsConfig.remove(getMainDocumentCollectionName());

        //add child docs
        for (String collectionName : collectionsConfig.keySet()) {
            IndexCollectionConfig collectionConfig = collectionsConfig.get(collectionName);
            NodeList rootNodes = (NodeList) xpath.evaluate(collectionConfig.getRootXpath(), xml, XPathConstants.NODESET);
            if (rootNodes == null || rootNodes.getLength() == 0)
                continue;
            for (int i = 0; i < rootNodes.getLength(); i++) {
                Node rootNode = rootNodes.item(i);

                SolrInputDocument doc = new SolrInputDocument();
                doc.addField(SolrArclibXmlDocument.COLLECTION_NAME, collectionConfig.getCollectionName());
                for (IndexFieldConfig conf : collectionConfig.getIndexedFieldConfig()) {
                    NodeList nodes = (NodeList) xpath.evaluate(conf.getXpath(), rootNode, XPathConstants.NODESET);
                    addFieldToDocument(doc, conf, nodes);
                }
                doc.addField(SolrArclibXmlDocument.ID, mainDocIdString + "_" + collectionName + "_" + i);
                mainDoc.addChildDocument(doc);
            }
        }

        try {
            solrTemplate.saveDocument(coreName, mainDoc);
            solrTemplate.commit(coreName);
        } catch (UncategorizedSolrException ex) {
            log.error(ex.getMessage());
            throw ex;
        }
    }

    @Override
    public Result<SolrArclibXmlDocument> findAll(Params params, String queryName) {
        SimpleQuery query = initializeQuery(params);
        query.addCriteria(Criteria.where(SolrArclibXmlDocument.COLLECTION_NAME).in(getMainDocumentCollectionName()));
        query.addFilterQuery(new SimpleFilterQuery(buildFilters(params)));
        log.info("Searching for documents");
        Page<SolrArclibXmlDocument> page;
        try {
            page = solrTemplate.query(coreName, query, SolrArclibXmlDocument.class);
        } catch (UncategorizedSolrException ex) {
            Matcher matcher = Pattern.compile(".+ undefined field (.+)").matcher(ex.getMessage());
            if (matcher.find()) {
                String msg = "query contains undefined field: " + matcher.group(1);
                throw new BadArgument(msg);
            }
            throw ex;
        }catch (DataAccessResourceFailureException ex){
            throw new BadArgument(ex.getMessage());
        }
        Result<SolrArclibXmlDocument> result = new Result<>();
        result.setItems(page.getContent());
        result.setCount(page.getTotalElements());
        if (queryName != null && !queryName.trim().isEmpty())
            aipQueryStore.save(new AipQuery(new User(userDetails.getId()), result, params, queryName));
        log.info("Found documents: " + Arrays.toString(result.getItems().stream().map(SolrArclibXmlDocument::getId).toArray()));
        return result;
    }

    @Override
    public void changeState(String arclibXmlDocumentId, IndexedArclibXmlDocumentState newState) {
        PartialUpdate partialUpdate = new PartialUpdate(SolrArclibXmlDocument.ID, arclibXmlDocumentId);
        partialUpdate.add(SolrArclibXmlDocument.STATE, newState.toString());
        solrTemplate.saveBean(coreName, partialUpdate);
        solrTemplate.commit(coreName);
    }

    @Override
    public void removeIndex(String id) {
        SolrDataQuery deleteQuery = new SimpleQuery("id:" + id + "*");
        solrTemplate.delete(coreName, deleteQuery);
        solrTemplate.commit(coreName);
    }

    /**
     * Finds ArclibXml index document by the external id and returns all the indexed attributes of the main document, OMMITING CHILDREN
     *
     * @param externalId external id of the ArclibXml index document
     * @return map of indexed attributes and their values
     */
    public Map<String, Object> findArclibXmlIndexDocument(String externalId) {
        SimpleQuery query = new SimpleQuery();
        query.addCriteria(Criteria.where(SolrArclibXmlDocument.ID).in(asSet(externalId)));
        log.info("Searching for document with id " + externalId);
        Page<SolrArclibXmlDocument> page = solrTemplate.query(coreName, query, SolrArclibXmlDocument.class);
        if (page.getContent().size() == 0) return null;
        return page.getContent().get(0).getFields();
    }

    private Criteria buildFilters(Params params) {
        if (params.getFilter() == null || params.getFilter().isEmpty())
            return Criteria.where("id");
        List<Criteria> queries = params.getFilter().stream()
                .map(this::buildFilter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (params.getOperation() == RootFilterOperation.OR) {
            return orQueryInternal(queries);
        } else {
            return andQueryInternal(queries);
        }
    }

    private Criteria buildFilter(Filter filter) {
        String value = sanitizeFilterValue(filter.getValue());
        FilterOperation operation = filter.getOperation();
        if (operation == null) {
            throw new BadArgument("operation not specified: " + filter);
        }
        if (value == null
                && operation != FilterOperation.AND
                && operation != FilterOperation.OR
                && operation != FilterOperation.NOT_NULL
                && operation != FilterOperation.IS_NULL
                && operation != FilterOperation.NESTED) {
            throw new BadArgument("value not specified: " + filter);
        }
        switch (operation) {
            case EQ:
            default:
                return inQuery(filter.getField(), asSet(value));
            case NEQ:
                return notInQuery(filter.getField(), asSet(value));
            case STARTWITH:
                return prefixQuery(filter.getField(), value);
            case ENDWITH:
                return suffixQuery(filter.getField(), value);
            case CONTAINS:
                return containsQuery(filter.getField(), value);
            case GT:
                return gtQuery(filter.getField(), value);
            case LT:
                return ltQuery(filter.getField(), value);
            case GTE:
                return gteQuery(filter.getField(), value);
            case LTE:
                return lteQuery(filter.getField(), value);
            case AND:
                return andQuery(filter.getFilter());
            case OR:
                return orQuery(filter.getFilter());
            case IS_NULL:
                return isNullQuery(filter.getField());
            case NOT_NULL:
                return notNullQuery(filter.getField());
            case NESTED:
                return nestedQuery(filter.getField(), filter.getFilter());
        }
    }

    /**
     * Builds an OR query between sub-filters.
     * <p>
     * Used internally in {@link SolrArclibXmlStore#findAll(Params, String)} )} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param filters {@link List} of {@link Filter}
     * @return Solr query builder
     */
    private Criteria orQuery(List<Filter> filters) {
        List<Criteria> builders = filters.stream()
                .map(this::buildFilter)
                .collect(Collectors.toList());
        return orQueryInternal(builders);
    }

    /**
     * Builds an AND query between sub-filters.
     * <p>
     * Used internally in {@link SolrArclibXmlStore#findAll(Params, String)} )} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param filters {@link List} of {@link Filter}
     * @return Solr query builder
     */
    private Criteria andQuery(List<Filter> filters) {
        List<Criteria> builders = filters.stream()
                .map(this::buildFilter)
                .collect(Collectors.toList());
        return andQueryInternal(builders);
    }

    /**
     * Creates namespace aware Xpath. Namespace URI must match URIs in ARCLib XML file.
     *
     * @return namespace aware XPath
     */
    private XPath getXpathWithNamespaceContext() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                if (prefix == null) {
                    throw new IllegalArgumentException("No prefix provided!");
                } else if (prefix.equalsIgnoreCase("METS")) {
                    return uris.get(METS);
                } else if (prefix.equalsIgnoreCase("oai_dc")) {
                    return uris.get(OAIS_DC);
                } else if (prefix.equalsIgnoreCase("premis")) {
                    return uris.get(PREMIS);
                } else if (prefix.equalsIgnoreCase("ARCLib")) {
                    return uris.get(ARCLIB);
                } else if (prefix.equalsIgnoreCase("dcterms")) {
                    return uris.get(DCTERMS);
                } else if (prefix.equalsIgnoreCase("dc")) {
                    return uris.get(DC);
                } else {
                    return XMLConstants.NULL_NS_URI;
                }
            }

            public String getPrefix(String namespaceURI) {
                // Not needed in this context.
                return null;
            }

            public Iterator getPrefixes(String namespaceURI) {
                // Not needed in this context.
                return null;
            }
        });
        return xpath;
    }

    private Criteria nestedQuery(String name, List<Filter> filters) {
        Criteria parentCriteria = Criteria.where(SolrArclibXmlDocument.COLLECTION_NAME).is(getMainDocumentCollectionName());
        try {
            Criteria childTypeCriteria = Criteria.where(SolrArclibXmlDocument.COLLECTION_NAME).is(name);
            Criteria childrenCriteria = andQuery(filters);
            childrenCriteria.and(childTypeCriteria);
            return new NestedCriteria(parentCriteria, childrenCriteria);
        } catch (Exception e) {
            throw new GeneralException(e);
        }
    }

    @Inject
    public void setUris(@Value("${namespaces.mets}") String mets, @Value("${namespaces.arclib}") String arclib, @Value("${namespaces" +
            ".premis}") String premis, @Value("${namespaces.oai_dc}") String oai_dc, @Value("${namespaces.dc}") String dc,
                        @Value("${namespaces.dcterms}") String dcterms) {
        Map<String, String> uris = new HashMap<>();
        uris.put(METS, mets);
        uris.put(ARCLIB, arclib);
        uris.put(PREMIS, premis);
        uris.put(OAIS_DC, oai_dc);
        uris.put(DC, dc);
        uris.put(DCTERMS, dcterms);

        this.uris = uris;
    }

    private void addFieldToDocument(SolrInputDocument doc, IndexFieldConfig conf, NodeList nodes) {
        if (nodes == null || nodes.getLength() == 0)
            return;
        for (int i = 0; i < nodes.getLength(); i++) {
            Calendar parsedDate;
            try {
                switch (conf.getFieldType()) {
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
                            doc.addField(conf.getFieldName(), nodeToString(nodes.item(i)));
                        }
                }
            } catch (IllegalArgumentException | NullPointerException | TransformerException parsingEx) {
                String msg = String.format("Could not parse %s as %s", nodes.item(i).getTextContent(), conf.getFieldType());
                throw new BadArgument(msg);
            }
        }
    }

    @Inject
    public void setAipQueryStore(AipQueryStore aipQueryStore) {
        this.aipQueryStore = aipQueryStore;
    }

    @Resource(name = "ArclibXmlSolrTemplate")
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
}
