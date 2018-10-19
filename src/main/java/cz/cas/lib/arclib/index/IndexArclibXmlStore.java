package cz.cas.lib.arclib.index;

import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocumentState;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface IndexArclibXmlStore<T> {

    /**
     * Creates index. {@link FieldType#TIME} are stored as count of milliseconds.
     *
     * @throws BadArgument if the value of {@link FieldType#TIME}, {@link FieldType#DATE} or {@link FieldType#DATETIME} field can't be parsed.
     */
    void createIndex(String arclibXml, String producerId, String userId, IndexedArclibXmlDocumentState state);

    /**
     * Finds documents.
     *
     * @param params params for filtering sorting etc.
     * @param save   whether to save query to use its result or the query itself later, see {@link AipQuery}
     * @return list of IDs of documents
     * @throws BadArgument if query contains field undefined in Solr schema.
     */
    Result<T> findAll(Params params, boolean save);

    /**
     * Find indexed fields of one document by ingest workflow external id.
     *
     * @param externalId
     * @return
     */
    Map<String, Object> findArclibXmlIndexDocument(String externalId);

    /**
     * Load configuration from a CSV file defining ARCLib XML.
     *
     * @return Set with config object for each index field.
     * @throws IOException
     */
    default Set<IndexFieldConfig> getFieldsConfig() throws IOException {
        Set<IndexFieldConfig> fieldConfigs = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("index/arclibXmlDefinition.csv")))) {
            String line = br.readLine();
            while (line != null) {
                String arr[] = line.split(",");
                if (arr.length != 8)
                    throw new IllegalArgumentException(String.format("arclibXmlDefinition.csv can't contain row with empty column: %s", line));
                if (!arr[6].equals("fulltext"))
                    fieldConfigs.add(new IndexFieldConfig(arr[6], arr[7], arr[1], "N".equals(arr[5])));
                line = br.readLine();
            }
        }
        return fieldConfigs;
    }
}
