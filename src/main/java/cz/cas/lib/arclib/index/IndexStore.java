package cz.cas.lib.arclib.index;

import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.index.dto.Filter;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface IndexStore {

    /**
     * Creates index. {@link FieldType#TIME} are stored as count of milliseconds.
     *
     * @throws BadArgument if the value of {@link FieldType#TIME}, {@link FieldType#DATE} or {@link FieldType#DATETIME} field can't be parsed.
     */
    void createIndex(String sipId, int xmlVersion, String arclibXml);

    /**
     * Finds documents.
     *
     * @return list of IDs of documents
     * @throws BadArgument if query contains field undefined in Solr schema.
     */
    List<String> findAll(List<Filter> filter);

    /**
     * Load configuration from a CSV file defining ARCLib XML.
     *
     * @return Set with config object for each index field.
     * @throws IOException
     */
    default Set<IndexFieldConfig> getFieldsConfig() throws IOException {
        List<String> fieldsDefinitions = Files.readAllLines(Paths.get("src/main/resources/index/fieldDefinitions.csv"));
        Set<IndexFieldConfig> fieldConfigs = new HashSet<>();
        for (String line :
                fieldsDefinitions.subList(1, fieldsDefinitions.size())) {
            String arr[] = line.split(",");
            if (arr.length != 8)
                throw new IllegalArgumentException(String.format("fieldDefinitions.csv can't contain row with empty column: %s", line));
            if (!arr[5].equals("fulltext") && !arr[5].equals("atribut"))
                fieldConfigs.add(new IndexFieldConfig(arr[5], arr[6], arr[7], "N" .equals(arr[2])));
        }
        return fieldConfigs;
    }

    /**
     * Creates namespace aware Xpath. Namespace URI must match URIs in ARCLib XMl file.
     *
     * @return namespace aware XPath
     */
    default XPath getXpathWithNamespaceContext() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                if (prefix == null) {
                    throw new IllegalArgumentException("No prefix provided!");
                } else if (prefix.equals("METS")) {
                    return "http://www.loc.gov/METS/";
                } else if (prefix.equals("oai_dc")) {
                    return "http://purl.org/dc/terms/";
                } else if (prefix.equals("premis")) {
                    return "info:lc/xmlns/premis-v2";
                } else if (prefix.equals("ARCLIB")) {
                    return "http://arclib.lib.cas.cz/ARCLIB_XSD";
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

    /**
     * Converts XML node to String.
     *
     * @param node
     * @return
     * @throws TransformerException
     */
    default String nodeToString(Node node)
            throws TransformerException {
        StringWriter buf = new StringWriter();
        Transformer xform = TransformerFactory.newInstance().newTransformer();
        xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        xform.transform(new DOMSource(node), new StreamResult(buf));
        return (buf.toString());
    }
}
