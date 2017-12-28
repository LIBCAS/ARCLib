package cz.cas.lib.arclib.solr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Dynamic;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SolrDocument(solrCoreName = "arclib_xml")
public class ArclibXmlDocument {

    /**
     * ID of document is id of SIP followed by underscore and XML version number.
     */
    @Field
    @Indexed
    private String id;

    /**
     * The whole document is indexed as fulltext.
     */
    @Field
    @Indexed
    private String document;

    /**
     * Fields of AIP XML which has to be indexed.
     */
    @Field("*")
    @Indexed
    @Dynamic
    private Map<String, Object> fields = new HashMap<>();

    /**
     * Adds field with its value to Solr document. If the field already exists values are stored in list.
     * @param fieldKey
     * @param newFieldValue
     */
    public void addField(String fieldKey, Object newFieldValue) {
        if (fields.containsKey(fieldKey)) {
            Object oldAttrValue = fields.get(fieldKey);
            if (oldAttrValue instanceof Set)
                ((HashSet) oldAttrValue).add(newFieldValue);
            else {
                Set<Object> fieldValues = new HashSet<>();
                fieldValues.add(fields.get(fieldKey));
                fieldValues.add(newFieldValue);
                fields.put(fieldKey, fieldValues);
            }
        } else
            fields.put(fieldKey, newFieldValue);
    }
}