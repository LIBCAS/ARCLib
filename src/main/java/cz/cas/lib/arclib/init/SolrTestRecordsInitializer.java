package cz.cas.lib.arclib.init;

import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocumentState;
import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlDocument;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlGenerator;
import cz.cas.lib.core.index.solr.util.TemporalConverters;
import cz.cas.lib.core.util.Utils;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cz.cas.lib.core.util.Utils.asSet;

@Service
public class SolrTestRecordsInitializer {
    @Resource(name = "ArclibXmlSolrTemplate")
    private SolrTemplate solrTemplate;
    @Value("${solr.arclibxml.corename}")
    private String coreName;

    public static final String XML1_ID = "ARCLIB_900000003";
    public static final String XML2_ID = "ARCLIB_900000004";
    public static final String XML3_ID = "ARCLIB_900000005";
    public static final String XML4_ID = "ARCLIB_900000006";
    public static final String XML5_ID = "ARCLIB_900000007";

    public static final String SIP1_ID = "4b66655a-819a-474f-8203-6c432815df1f";
    public static final String SIP2_ID = "8b2efafd-b637-4b97-a8f7-1b97dd4ee622";
    public static final String SIP3_ID = "89f82da0-af78-4461-bf92-7382050082a1";

    public static final String USER_ID = "8ccec52b-fd85-49a4-bc0b-aaa56822701e";
    public static final String USER_NAME = "arc-bob";
    public static final String PRODUCER_ID = "aa7ddcc5-5b81-4747-bfeb-1850d952a359";
    public static final String PRODUCER_NAME = "Producer 1";

    public static final String AUTHORIAL_ID = "authorialId3";
    public static final String AUTHORIAL_ID2 = "authorialId4";


    public void init() throws IOException {
        SimpleQuery query = new SimpleQuery();
        query.addCriteria(Criteria.where("id"));
        solrTemplate.delete(coreName, query);
        solrTemplate.commit(coreName);
        List<SolrInputDocument> documents = new ArrayList<>();
        Map<String, Object> otherFields1 = new HashMap<>();
        otherFields1.put("premis_event_type", asSet("validation", "ingestion"));

        Map<String, Object> otherFields2 = new HashMap<>();
        otherFields2.put("label", asSet("some label"));

        String arclibXml1 = Utils.resourceString("sampleData/4b66655a-819a-474f-8203-6c432815df1f_xml_1.xml");
        String arclibXml2 = Utils.resourceString("sampleData/4b66655a-819a-474f-8203-6c432815df1f_xml_2.xml");
        String arclibXml3 = Utils.resourceString("sampleData/8b2efafd-b637-4b97-a8f7-1b97dd4ee622_xml_1.xml");
        String arclibXml4 = Utils.resourceString("sampleData/8b2efafd-b637-4b97-a8f7-1b97dd4ee622_xml_2.xml");
        String arclibXml5 = Utils.resourceString("sampleData/89f82da0-af78-4461-bf92-7382050082a1_xml_1.xml");

        documents.add(SolrArclibXmlDocument.createDocument(XML1_ID, TemporalConverters.isoStringToDate("2018-03-08T10:00:00Z"),
                PRODUCER_ID, PRODUCER_NAME, USER_NAME, AUTHORIAL_ID, SIP1_ID, 1, 1,
                ArclibXmlGenerator.INITIAL_VERSION, ArclibXmlGenerator.INITIAL_VERSION, arclibXml1, IndexedArclibXmlDocumentState.PERSISTED, false, "parent", otherFields1));
        documents.add(SolrArclibXmlDocument.createDocument(XML2_ID, TemporalConverters.isoStringToDate("2018-03-08T11:00:00Z"),
                PRODUCER_ID, PRODUCER_NAME, USER_NAME, AUTHORIAL_ID, SIP1_ID, 1, 2,
                ArclibXmlGenerator.INITIAL_VERSION, XML1_ID, arclibXml2, IndexedArclibXmlDocumentState.PERSISTED, false, "parent", otherFields2));
        documents.add(SolrArclibXmlDocument.createDocument(XML3_ID, TemporalConverters.isoStringToDate("2018-03-08T12:00:00Z"),
                PRODUCER_ID, PRODUCER_NAME, USER_NAME, AUTHORIAL_ID, SIP2_ID, 2, 1,
                SIP1_ID, ArclibXmlGenerator.INITIAL_VERSION, arclibXml3, IndexedArclibXmlDocumentState.PERSISTED, false, "parent", otherFields1));
        documents.add(SolrArclibXmlDocument.createDocument(XML4_ID, TemporalConverters.isoStringToDate("2018-03-08T13:00:00Z"),
                PRODUCER_ID, PRODUCER_NAME, USER_NAME, AUTHORIAL_ID, SIP2_ID, 2, 2,
                SIP1_ID, XML3_ID, arclibXml4, IndexedArclibXmlDocumentState.PERSISTED, false, "parent", otherFields2));
        documents.add(SolrArclibXmlDocument.createDocument(XML5_ID, TemporalConverters.isoStringToDate("2018-03-08T14:00:00Z"),
                PRODUCER_ID, PRODUCER_NAME, USER_NAME, AUTHORIAL_ID2, SIP3_ID, 1, 1,
                ArclibXmlGenerator.INITIAL_VERSION, ArclibXmlGenerator.INITIAL_VERSION, arclibXml5, IndexedArclibXmlDocumentState.PERSISTED, false, "parent", otherFields2));

        solrTemplate.saveDocuments(coreName, documents);
        solrTemplate.commit(coreName);
    }
}
