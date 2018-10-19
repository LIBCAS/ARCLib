package cz.cas.lib.core.index.solr.util;

import org.apache.solr.common.SolrInputDocument;
import org.springframework.data.solr.core.convert.SolrJConverter;

import java.util.List;
import java.util.Map;

/**
 * Extends SolrJ Converter so child Documents are not ignored
 */
public class MySolrJConverter extends SolrJConverter {
    @Override
    public void write(Object source, Map sink) {
        if (source == null) {
            return;
        }

        SolrInputDocument convertedDocument = convert(source, SolrInputDocument.class);
        sink.putAll(convertedDocument);

        if (sink instanceof SolrInputDocument) {
            SolrInputDocument parent = (SolrInputDocument) sink;

            List<SolrInputDocument> childDocuments = convertedDocument.getChildDocuments();

            if (childDocuments != null) {
                parent.addChildDocuments(childDocuments);
            }
        }
    }
}
