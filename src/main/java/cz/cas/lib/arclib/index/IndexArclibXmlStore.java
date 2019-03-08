package cz.cas.lib.arclib.index;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocumentState;
import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlDocument;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public interface IndexArclibXmlStore<T> {

    /**
     * Creates index. {@link FieldType#TIME} are stored as count of milliseconds.
     *
     * @throws BadArgument if the value of {@link FieldType#TIME}, {@link FieldType#DATE} or {@link FieldType#DATETIME} field can't be parsed.
     */
    void createIndex(String arclibXml, Producer producer, User user, IndexedArclibXmlDocumentState state, boolean debuggingModeActive);

    /**
     * Finds documents.
     *
     * @param params params for filtering sorting etc.
     * @param queryName if queryName is set, the query is saved to use its result or the query itself later
     * @return list of IDs of documents
     * @throws BadArgument if query contains field undefined in Solr schema.
     */
    Result<SolrArclibXmlDocument> findAll(Params params, String queryName);

    /**
     * Find indexed fields of one document by ingest workflow external id.
     *
     * @param externalId
     * @return
     */
    Map<String, Object> findArclibXmlIndexDocument(String externalId);

    /**
     * changes the state of the record
     */
    void changeState(String arclibXmlDocumentId, IndexedArclibXmlDocumentState newState);

    void removeIndex(String id);

    /**
     * Load configuration from a CSV file defining ARCLib XML.
     *
     * @return Set with config object for each index field.
     * @throws IOException
     */

    default String getMainDocumentCollectionName() {
        return "parent";
    }

    default Map<String, IndexCollectionConfig> getArclibXmlCollectionsConfig() throws IOException {
        IndexCollectionConfig mainCollection = new IndexCollectionConfig(null, getMainDocumentCollectionName());
        Map<String, IndexCollectionConfig> collectionNameToConfigMap = new HashMap<>();
        collectionNameToConfigMap.put(mainCollection.getCollectionName(), mainCollection);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("index/arclibXmlDefinition.csv")))) {
            br.readLine();
            String line = br.readLine();
            while (line != null) {
                String arr[] = line.split(",", -1);
                if (arr.length != 9)
                    throw new IllegalArgumentException(String.format("wrong format of ARCLib xml definition line: %s", line));
                if (!arr[6].isEmpty() && arr[8].isEmpty()) {
                    mainCollection.getIndexedFieldConfig().add(new IndexFieldConfig(arr[6], arr[7], arr[1], !"N".equals(arr[5])));
                    line = br.readLine();
                    continue;
                }
                if (arr[6].isEmpty() && !arr[8].isEmpty() && "N".equals(arr[5])) {
                    collectionNameToConfigMap.put(arr[8], new IndexCollectionConfig(arr[1], arr[8]));
                    line = br.readLine();
                    continue;
                }
                if (!arr[6].isEmpty() && !arr[8].isEmpty()) {
                    IndexCollectionConfig childConfig = collectionNameToConfigMap.get(arr[8]);
                    if (childConfig == null)
                        throw new IllegalArgumentException("Found config line for field: " + arr[6] + " of child: " + arr[8] + " but the child was not defined yet. There must be a child defining line preceeding this one with: 1) xpath set to the root of the child, 2) multiplicity set to N, 3) empty field name and field type, 4) child name set to: " + arr[8]);
                    if (!arr[1].startsWith(childConfig.getRootXpath()))
                        throw new IllegalArgumentException("Found config line for field: " + arr[6] + " of child: " + arr[8] + " which xpath: " + arr[1] + " does not have the prefix equal to the root xpath: " + childConfig.getRootXpath());
                    String childRelativeXpath = arr[1].replace(childConfig.getRootXpath(), "");
                    if (childRelativeXpath.charAt(0) == '/' && childRelativeXpath.charAt(1) != '/') {
                        childRelativeXpath = childRelativeXpath.substring(1);
                    }
                    childConfig.getIndexedFieldConfig().add(new IndexFieldConfig(arr[6], arr[7], childRelativeXpath, !"N".equals(arr[5])));
                }
                line = br.readLine();
            }
        }
        return collectionNameToConfigMap;
    }
}
