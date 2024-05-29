package cz.cas.lib.arclib.index;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.ConflictException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IndexedArclibXmlStore {

    /**
     * Creates index. {@link AipXmlNodeValueType#TIME} are stored as count of milliseconds.
     *
     * @throws BadArgument if the value of {@link AipXmlNodeValueType#TIME}, {@link AipXmlNodeValueType#DATE} or {@link AipXmlNodeValueType#DATETIME} field can't be parsed.
     */
    void createIndex(CreateIndexRecordDto createIndexRecordDto);

    /**
     * Finds documents.
     *
     * @param params params for filtering sorting etc.
     * @return search result
     * @throws BadArgument if query contains field undefined in Solr schema.
     */
    Result<IndexedArclibXmlDocument> findAll(Params params);

    /**
     * Finds all documents and ignores pagination.
     *
     * @param params params for filtering sorting etc.
     * @return search result
     * @throws BadArgument if query contains field undefined in Solr schema.
     */
    Result<IndexedArclibXmlDocument> findAllIgnorePagination(Params params);

    /**
     * Finds single ArclibXml index document by the external id, or throws exception if zero or more documents were found
     *
     * @param externalId external id of the ArclibXml index document
     * @return map of indexed attributes and their values
     * @throws MissingObject
     * @throws ConflictException
     */
    IndexedArclibXmlDocument findArclibXmlIndexDocument(String externalId);

    /**
     * changes the aip state of the record
     */
    void changeAipState(String arclibXmlDocumentId, IndexedAipState newState, byte[] aipXml);

    /**
     * updates the <i>latest</i> and <i>latestData</i> flags of the ARCLib XML record
     */
    void setLatestFlags(SetLatestFlagsDto setLatestFlagsDto);

    void removeIndex(String id);

    String getMainDocumentIndexType();

    Resource getArclibXmlIndexConfig();

    List<IndexedArclibXmlDocument> findWithChildren(Collection<String> docIds, List<SimpleIndexFilter> additionalFilters);

    /**
     * Fills passed attributes with config parsed from arclibXmlIndexConfig.csv
     *
     * @param mainCollection       config of the main collection
     * @param indexTypeToConfigMap map containing collection names (main and nested) as keys and their configurations as values
     * @throws IOException
     */
    default void parseCsvConfig(ArclibXmlIndexTypeConfig mainCollection, Map<String, ArclibXmlIndexTypeConfig> indexTypeToConfigMap) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(getArclibXmlIndexConfig().getInputStream(), StandardCharsets.UTF_8));
        Iterable<CSVRecord> records = CSVFormat.EXCEL.withDelimiter(',').withHeader().withSkipHeaderRecord(true).parse(br);

        for (CSVRecord record : records) {
            String xpath = record.get(1);
            boolean indexSimple = "N".equals(record.get(3));
            String indexFieldName = record.get(4);
            String indexType = record.get(5);
            String indexParentName = record.get(6);

            //configuration of fields of main collection (i.e. not nested fields)
            if (!indexFieldName.isEmpty() && indexParentName.isEmpty()) {
                mainCollection.getIndexedFieldConfig().add(new ArclibXmlField(indexFieldName, indexType, xpath, !indexSimple));
                continue;
            }
            //declaration of nested collection, configuration of its fields follows
            if (indexFieldName.isEmpty() && !indexParentName.isEmpty() && indexSimple) {
                indexTypeToConfigMap.put(indexParentName, new ArclibXmlIndexTypeConfig(xpath, indexParentName));
                continue;
            }
            //configuration of fields of particular nested collection
            if (!indexFieldName.isEmpty() && !indexParentName.isEmpty()) {
                ArclibXmlIndexTypeConfig childConfig = indexTypeToConfigMap.get(indexParentName);
                if (childConfig == null)
                    throw new IllegalArgumentException("Found config line for field: " + indexFieldName + " of child: " + indexParentName + " but the child was not defined yet. There must be a child defining line preceeding this one with: 1) xpath set to the root of the child, 2) multiplicity set to N, 3) empty field name and field type, 4) child name set to: " + indexParentName);
                if (!xpath.startsWith(childConfig.getRootXpath()))
                    throw new IllegalArgumentException("Found config line for field: " + indexFieldName + " of child: " + indexParentName + " which xpath: " + xpath + " does not have the prefix equal to the root xpath: " + childConfig.getRootXpath());
                String childRelativeXpath = xpath.replace(childConfig.getRootXpath(), "");
                if (childRelativeXpath.charAt(0) == '/' && childRelativeXpath.charAt(1) != '/') {
                    childRelativeXpath = childRelativeXpath.substring(1);
                }
                childConfig.getIndexedFieldConfig().add(new ArclibXmlField(indexFieldName, indexType, childRelativeXpath, !indexSimple));
            }
        }
    }
}
