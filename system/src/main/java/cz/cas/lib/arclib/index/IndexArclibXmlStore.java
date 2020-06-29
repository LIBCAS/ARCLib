package cz.cas.lib.arclib.index;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public interface IndexArclibXmlStore<T> {

    /**
     * Creates index. {@link AipXmlNodeValueType#TIME} are stored as count of milliseconds.
     *
     * @throws BadArgument if the value of {@link AipXmlNodeValueType#TIME}, {@link AipXmlNodeValueType#DATE} or {@link AipXmlNodeValueType#DATETIME} field can't be parsed.
     */
    void createIndex(byte[] arclibXml, String producerId, String producerName, String userName, IndexedAipState aipState, boolean debuggingModeActive, boolean latestVersion);

    /**
     * Finds documents.
     *
     * @param params params for filtering sorting etc.
     * @param queryName if queryName is set, the query is saved to use its result or the query itself later
     * @return list of IDs of documents
     * @throws BadArgument if query contains field undefined in Solr schema.
     */
    Result<IndexedArclibXmlDocument> findAll(Params params, String queryName);

    /**
     * Find indexed fields of one document by ingest workflow external id.
     *
     * @param externalId
     * @return
     */
    Map<String, Object> findArclibXmlIndexDocument(String externalId);

    /**
     * changes the aip state of the record
     */
    void changeAipState(String arclibXmlDocumentId, IndexedAipState newState, byte[] aipXml);

    /**
     * updates the <i>latest</i> flag of the ARCLib XML record
     */
    void setLatestFlag(String arclibXmlDocumentId, boolean flag, byte[] aipXml);

    void removeIndex(String id);

    String getMainDocumentIndexType();

    Resource getArclibXmlDefinition();

    /**
     * parses CSV file into configuration used by indexer
     *
     * @return map containing collection name and its configuration filled with data of main collection and also data of all nested collections
     * @throws IOException
     */
    default Map<String, ArclibXmlIndexTypeConfig> getArclibXmlCollectionsConfig() throws IOException {
        //declaration of main collection
        ArclibXmlIndexTypeConfig mainCollection = new ArclibXmlIndexTypeConfig(null, getMainDocumentIndexType());
        Map<String, ArclibXmlIndexTypeConfig> indexTypeToConfigMap = new HashMap<>();
        indexTypeToConfigMap.put(mainCollection.getIndexType(), mainCollection);

        BufferedReader br = new BufferedReader(new InputStreamReader(getArclibXmlDefinition().getInputStream(), StandardCharsets.UTF_8));
        Iterable<CSVRecord> records = CSVFormat.EXCEL.withDelimiter(',').withHeader().withSkipHeaderRecord(true).parse(br);

        for (CSVRecord record : records) {
            //configuration of fields of main collection (i.e. not nested fields)
            if (!record.get(6).isEmpty() && record.get(8).isEmpty()) {
                mainCollection.getIndexedFieldConfig().add(new ArclibXmlField(record.get(6), record.get(7), record.get(1), !"N".equals(record.get(5))));
                continue;
            }
            //declaration of nested collection, configuration of its fields follows
            if (record.get(6).isEmpty() && !record.get(8).isEmpty() && "N".equals(record.get(5))) {
                indexTypeToConfigMap.put(record.get(8), new ArclibXmlIndexTypeConfig(record.get(1), record.get(8)));
                continue;
            }
            //configuration of fields of particular nested collection
            if (!record.get(6).isEmpty() && !record.get(8).isEmpty()) {
                ArclibXmlIndexTypeConfig childConfig = indexTypeToConfigMap.get(record.get(8));
                if (childConfig == null)
                    throw new IllegalArgumentException("Found config line for field: " + record.get(6) + " of child: " + record.get(8) + " but the child was not defined yet. There must be a child defining line preceeding this one with: 1) xpath set to the root of the child, 2) multiplicity set to N, 3) empty field name and field type, 4) child name set to: " + record.get(8));
                if (!record.get(1).startsWith(childConfig.getRootXpath()))
                    throw new IllegalArgumentException("Found config line for field: " + record.get(6) + " of child: " + record.get(8) + " which xpath: " + record.get(1) + " does not have the prefix equal to the root xpath: " + childConfig.getRootXpath());
                String childRelativeXpath = record.get(1).replace(childConfig.getRootXpath(), "");
                if (childRelativeXpath.charAt(0) == '/' && childRelativeXpath.charAt(1) != '/') {
                    childRelativeXpath = childRelativeXpath.substring(1);
                }
                childConfig.getIndexedFieldConfig().add(new ArclibXmlField(record.get(6), record.get(7), childRelativeXpath, !"N".equals(record.get(5))));
            }
        }
        return indexTypeToConfigMap;
    }
}
