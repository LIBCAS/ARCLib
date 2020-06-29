package cz.cas.lib.arclib.init;

import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlStore;
import cz.cas.lib.core.util.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.io.IOException;

@Service
public class SolrTestRecordsInitializer {
    @Resource(name = "ArclibXmlSolrTemplate")
    private SolrTemplate solrTemplate;
    @Value("${solr.arclibxml.corename}")
    private String coreName;
    @Inject
    private IndexedArclibXmlStore indexedArclibXmlStore;

    public static final String USER_ID = "8ccec52b-fd85-49a4-bc0b-aaa56822701e";
    public static final String USER_NAME = "arc-bob";
    public static final String PRODUCER_ID = "aa7ddcc5-5b81-4747-bfeb-1850d952a359";
    public static final String PRODUCER_NAME = "Producer 1";


    public void init() throws IOException {
        SimpleQuery query = new SimpleQuery();
        query.addCriteria(Criteria.where("id"));
        solrTemplate.delete(coreName, query);
        solrTemplate.commit(coreName);

        String arclibXml1 = Utils.resourceString("sampleData/4b/66/65/4b66655a-819a-474f-8203-6c432815df1f_xml_1");
        String arclibXml2 = Utils.resourceString("sampleData/4b/66/65/4b66655a-819a-474f-8203-6c432815df1f_xml_2");
        String arclibXml3 = Utils.resourceString("sampleData/8b/2e/fa/8b2efafd-b637-4b97-a8f7-1b97dd4ee622_xml_1");
        String arclibXml4 = Utils.resourceString("sampleData/8b/2e/fa/8b2efafd-b637-4b97-a8f7-1b97dd4ee622_xml_2");
        String arclibXml5 = Utils.resourceString("sampleData/89/f8/2d/89f82da0-af78-4461-bf92-7382050082a1_xml_1");

        indexedArclibXmlStore.createIndex(arclibXml1.getBytes(), PRODUCER_ID, PRODUCER_NAME, USER_NAME, IndexedAipState.ARCHIVED, false, false);
        indexedArclibXmlStore.createIndex(arclibXml2.getBytes(), PRODUCER_ID, PRODUCER_NAME, USER_NAME, IndexedAipState.ARCHIVED, false, false);
        indexedArclibXmlStore.createIndex(arclibXml3.getBytes(), PRODUCER_ID, PRODUCER_NAME, USER_NAME, IndexedAipState.ARCHIVED, false, false);
        indexedArclibXmlStore.createIndex(arclibXml4.getBytes(), PRODUCER_ID, PRODUCER_NAME, USER_NAME, IndexedAipState.ARCHIVED, false, true);
        indexedArclibXmlStore.createIndex(arclibXml5.getBytes(), PRODUCER_ID, PRODUCER_NAME, USER_NAME, IndexedAipState.ARCHIVED, false, true);
    }
}
