package cz.cas.lib.arclib.index.solr.arclibxml;

import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.index.IndexArclibXmlStore;
import cz.cas.lib.arclib.index.IndexFieldConfig;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.AipQueryStore;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.index.dto.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.solr.UncategorizedSolrException;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
    public void createIndex(String arclibXml, String producerId, String userId, IndexedArclibXmlDocumentState state) {
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
        SolrArclibXmlDocument solrSolrArclibXmlDocument = new SolrArclibXmlDocument();

        for (IndexFieldConfig conf : getFieldsConfig()) {
            NodeList fields = (NodeList) xpath.evaluate(conf.getXpath(), xml, XPathConstants.NODESET);
            if (fields == null || fields.getLength() == 0)
                continue;
            switch (conf.getFieldName()) {
                case SolrArclibXmlDocument.ID:
                    solrSolrArclibXmlDocument.setId(fields.item(0).getTextContent());
                    continue;
                case SolrArclibXmlDocument.CREATED:
                    Calendar parsedDate = DatatypeConverter.parseDateTime(fields.item(0).getTextContent());
                    java.util.Date created = Date.from(parsedDate.toInstant().atOffset(ZoneOffset.UTC).toInstant());
                    solrSolrArclibXmlDocument.setCreated(created);
                    continue;
                case SolrArclibXmlDocument.SIP_ID:
                    solrSolrArclibXmlDocument.setSipId(fields.item(0).getTextContent());
                    continue;
                case SolrArclibXmlDocument.AUTHORIAL_ID:
                    solrSolrArclibXmlDocument.setAuthorialId(fields.item(0).getTextContent());
                    continue;
                case SolrArclibXmlDocument.SIP_VERSION_NUMBER:
                    solrSolrArclibXmlDocument.setSipVersionNumber(Integer.valueOf(fields.item(0).getTextContent()));
                    continue;
                case SolrArclibXmlDocument.XML_VERSION_NUMBER:
                    solrSolrArclibXmlDocument.setXmlVersionNumber(Integer.valueOf(fields.item(0).getTextContent()));
                    continue;
                case SolrArclibXmlDocument.SIP_VERSION_OF:
                    solrSolrArclibXmlDocument.setSipVersionOf(fields.item(0).getTextContent());
                    continue;
                case SolrArclibXmlDocument.XML_VERSION_OF:
                    solrSolrArclibXmlDocument.setXmlVersionOf(fields.item(0).getTextContent());
            }

            for (int i = 0; i < fields.getLength(); i++) {
                Calendar parsedDate;
                try {
                    switch (conf.getFieldType()) {
                        case DATETIME:
                            parsedDate = DatatypeConverter.parseDateTime(fields.item(i).getTextContent());
                            String parsedDateTimeString = DateTimeFormatter.ISO_INSTANT.format(parsedDate.toInstant().atOffset(ZoneOffset.UTC));
                            solrSolrArclibXmlDocument.addField(conf.getFieldName(), parsedDateTimeString);
                            break;
                        case DATE:
                            parsedDate = DatatypeConverter.parseDate(fields.item(i).getTextContent());
                            String parsedDateString = DateTimeFormatter.ISO_INSTANT.format(parsedDate.toInstant().atOffset(ZoneOffset.UTC));
                            solrSolrArclibXmlDocument.addField(conf.getFieldName(), parsedDateString);
                            break;
                        case TIME:
                            ZonedDateTime time = DatatypeConverter.parseTime(fields.item(i).getTextContent()).toInstant().atZone(ZoneId.systemDefault());
                            long parsedTime = time.getHour() * 60 * 60 * 1000 + time.getMinute() * 60 * 1000 + time.getSecond() * 1000;
                            solrSolrArclibXmlDocument.addField(conf.getFieldName(), parsedTime);
                            break;
                        default:
                            if (conf.isFullText()) {
                                solrSolrArclibXmlDocument.addField(conf.getFieldName(), nodeToString(fields.item(i)));
                            } else {
                                solrSolrArclibXmlDocument.addField(conf.getFieldName(), fields.item(i).getTextContent());
                            }
                    }
                } catch (IllegalArgumentException | NullPointerException parsingEx) {
                    String msg = String.format("Could not parse %s as %s", fields.item(i).getTextContent(), conf.getFieldType());
                    throw new BadArgument(msg);
                }
            }
        }
        solrSolrArclibXmlDocument.setProducerId(producerId);
        solrSolrArclibXmlDocument.setUserId(userId);
        solrSolrArclibXmlDocument.setDocument(arclibXml);
        solrSolrArclibXmlDocument.setState(state);
        try {
            solrTemplate.saveBean(coreName, solrSolrArclibXmlDocument);
            solrTemplate.commit(coreName);
        } catch (UncategorizedSolrException ex) {
            log.error(ex.getMessage());
            throw ex;
        }
    }

    @Override
    public Result<SolrArclibXmlDocument> findAll(Params params, boolean save) {
        SimpleQuery query = initializeQuery(params);
        query.addCriteria(Criteria.where(SolrArclibXmlDocument.ID));
        query.addFilterQuery(new SimpleFilterQuery(buildFilters(params)));
        log.info("Searching for documents");
        Page<SolrArclibXmlDocument> page;
        try {
            page = solrTemplate.query(coreName, query, SolrArclibXmlDocument.class);
        } catch (UncategorizedSolrException ex) {
            Matcher matcher = Pattern.compile(".+ undefined field (.+)").matcher(ex.getMessage());
            if (matcher.find()) {
                String msg = "query contains undefined field: " + matcher.group(1);
                log.error(msg);
                throw new BadArgument(msg);
            }
            throw ex;
        }
        Result<SolrArclibXmlDocument> result = new Result<>();
        result.setItems(page.getContent());
        result.setCount(page.getTotalElements());
        if (save)
            aipQueryStore.save(new AipQuery(new User(userDetails.getId()), result, params));
        log.info("Found documents: " + Arrays.toString(result.getItems().stream().map(SolrArclibXmlDocument::getId).toArray()));
        return result;
    }

    /**
     * Finds ArclibXml index document by the external id and returns all the indexed attributes
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
        if (operation == FilterOperation.NESTED) {
            throw new BadArgument("operation not supported: " + filter);
        }
        if (value == null
                && operation != FilterOperation.AND
                && operation != FilterOperation.OR
                && operation != FilterOperation.NOT_NULL
                && operation != FilterOperation.IS_NULL) {
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
        }
    }

    /**
     * Builds an OR query between sub-filters.
     * <p>
     * Used internally in {@link SolrArclibXmlStore#findAll(Params, boolean)} )} or in custom search methods in inheriting classes.
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
     * Used internally in {@link SolrArclibXmlStore#findAll(Params, boolean)} )} or in custom search methods in inheriting classes.
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
