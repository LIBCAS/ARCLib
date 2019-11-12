package cz.cas.lib.arclib.index;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public interface IndexArclibXmlStore<T> {

    /**
     * Creates index. {@link AipXmlNodeValueType#TIME} are stored as count of milliseconds.
     *
     * @throws BadArgument if the value of {@link AipXmlNodeValueType#TIME}, {@link AipXmlNodeValueType#DATE} or {@link AipXmlNodeValueType#DATETIME} field can't be parsed.
     */
    void createIndex(String arclibXml, String producerId, String producerName, String userName, IndexedAipState aipState, boolean debuggingModeActive);

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
    void changeAipState(String arclibXmlDocumentId, IndexedAipState newState);

    void removeIndex(String id);

    String getMainDocumentIndexType();

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
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("index/arclibXmlDefinition.csv")))) {
            br.readLine();
            String line = br.readLine();
            while (line != null) {
                String arr[] = line.split(",", -1);
                if (arr.length != 8)
                    throw new IllegalArgumentException(String.format("wrong format of ARCLib xml definition line: %s", line));
                //configuration of fields of main collection (i.e. not nested fields)
                if (!arr[5].isEmpty() && arr[7].isEmpty()) {
                    mainCollection.getIndexedFieldConfig().add(new ArclibXmlField(arr[5], arr[6], arr[1], !"N".equals(arr[4])));
                    line = br.readLine();
                    continue;
                }
                //declaration of nested collection, configuration of its fields follows
                if (arr[5].isEmpty() && !arr[7].isEmpty() && "N".equals(arr[4])) {
                    indexTypeToConfigMap.put(arr[7], new ArclibXmlIndexTypeConfig(arr[1], arr[7]));
                    line = br.readLine();
                    continue;
                }
                //configuration of fields of particular nested collection
                if (!arr[5].isEmpty() && !arr[7].isEmpty()) {
                    ArclibXmlIndexTypeConfig childConfig = indexTypeToConfigMap.get(arr[7]);
                    if (childConfig == null)
                        throw new IllegalArgumentException("Found config line for field: " + arr[5] + " of child: " + arr[7] + " but the child was not defined yet. There must be a child defining line preceeding this one with: 1) xpath set to the root of the child, 2) multiplicity set to N, 3) empty field name and field type, 4) child name set to: " + arr[7]);
                    if (!arr[1].startsWith(childConfig.getRootXpath()))
                        throw new IllegalArgumentException("Found config line for field: " + arr[5] + " of child: " + arr[7] + " which xpath: " + arr[1] + " does not have the prefix equal to the root xpath: " + childConfig.getRootXpath());
                    String childRelativeXpath = arr[1].replace(childConfig.getRootXpath(), "");
                    if (childRelativeXpath.charAt(0) == '/' && childRelativeXpath.charAt(1) != '/') {
                        childRelativeXpath = childRelativeXpath.substring(1);
                    }
                    childConfig.getIndexedFieldConfig().add(new ArclibXmlField(arr[5], arr[6], childRelativeXpath, !"N".equals(arr[4])));
                }
                line = br.readLine();
            }
        }
        return indexTypeToConfigMap;
    }
}
