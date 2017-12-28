package cz.cas.lib.arclib.solr;

import cz.cas.lib.arclib.index.Filter;
import cz.cas.lib.arclib.index.IndexFieldConfig;
import cz.cas.lib.arclib.index.IndexStore;
import cz.inqool.uas.exception.BadArgument;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.common.SolrInputDocument;
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

import javax.inject.Inject;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SolrStore implements IndexStore {

    private SolrTemplate solrTemplate;

    private ArclibXmlRepository arclibXmlRepository;

    @SneakyThrows
    public void createIndex(String sipId, int xmlVersion, String arclibXml) {
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
        ArclibXmlDocument solrArclibXmlDocument = new ArclibXmlDocument();

        SolrInputDocument doc = new SolrInputDocument();
        for (IndexFieldConfig conf : getFieldsConfig()) {
            NodeList fields = (NodeList) xpath.evaluate(conf.getXpath(), xml, XPathConstants.NODESET);

            for (int i = 0; i < fields.getLength(); i++) {
                try {
                    switch (conf.getFieldType()) {
                        case DATETIME:
                            Calendar parsedDate = DatatypeConverter.parseDateTime(fields.item(i).getTextContent());
                            String parsedDateTimeString = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(parsedDate.toInstant().atZone(ZoneId.systemDefault()));
                            solrArclibXmlDocument.addField(conf.getFieldName(), parsedDateTimeString);
                            break;
                        case DATE:
                            parsedDate = DatatypeConverter.parseDate(fields.item(i).getTextContent());
                            String parsedDateString = DateTimeFormatter.ISO_LOCAL_DATE.format(parsedDate.toInstant().atZone(ZoneId.systemDefault()));
                            solrArclibXmlDocument.addField(conf.getFieldName(), parsedDateString);
                            break;
                        case TIME:
                            ZonedDateTime time = DatatypeConverter.parseTime(fields.item(i).getTextContent()).toInstant().atZone(ZoneId.systemDefault());
                            long parsedTime = time.getHour() * 60 * 60 * 1000 + time.getMinute() * 60 * 1000 + time.getSecond() * 1000;
                            solrArclibXmlDocument.addField(conf.getFieldName(), parsedTime);
                            break;
                        default:
                            if (conf.isFullText()) {
                                solrArclibXmlDocument.addField(conf.getFieldName(), nodeToString(fields.item(i)));
                            } else {
                                solrArclibXmlDocument.addField(conf.getFieldName(), fields.item(i).getTextContent());
                            }
                    }
                } catch (IllegalArgumentException | NullPointerException parsingEx) {
                    String msg = String.format("Could not parse %s as %s", fields.item(i).getTextContent(), conf.getFieldType());
                    log.error(msg);
                    throw new BadArgument(msg);
                }
            }

        }
        solrArclibXmlDocument.setId(sipId + "_" + xmlVersion);
        solrArclibXmlDocument.setDocument(arclibXml);
        try {
            arclibXmlRepository.save(solrArclibXmlDocument);
        } catch (UncategorizedSolrException ex) {
            log.error(ex.getMessage());
            throw ex;
        }
    }

    public List<String> findAll(List<Filter> filter) {
        SimpleQuery query = new SimpleQuery(Criteria.where("id"));
        query.addProjectionOnField("id");
        if (filter.size() > 0)
            query.addFilterQuery(new SimpleFilterQuery(SolrQueryBuilder.buildFilters(filter)));
        log.info("searching for documents");
        Page<ArclibXmlDocument> page;
        try {
            page = solrTemplate.query(query, ArclibXmlDocument.class);
        } catch (UncategorizedSolrException ex) {
            Matcher matcher = Pattern.compile(".+ undefined field (.+)").matcher(ex.getMessage());
            if (matcher.find()) {
                String msg = "query contains undefined field: " + matcher.group(1);
                log.error(msg);
                throw new BadArgument(msg);
            }
            throw ex;
        }
        List<String> ids = page.getContent().stream().map(ArclibXmlDocument::getId).collect(Collectors.toList());
        ids.stream().forEach(id -> log.info("found doc with id: " + id));
        return ids;
    }

    @Inject
    public void setSolrTemplate(SolrTemplate solrTemplate) {
        this.solrTemplate = solrTemplate;
    }

    @Inject
    public void setArclibXmlRepository(ArclibXmlRepository arclibXmlRepository) {
        this.arclibXmlRepository = arclibXmlRepository;
    }
}
