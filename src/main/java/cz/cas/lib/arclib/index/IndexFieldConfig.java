package cz.cas.lib.arclib.index;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration class used when indexing.
 */
@Getter
@Setter
@AllArgsConstructor
public class IndexFieldConfig {

    private String fieldName;
    private FieldType fieldType;
    /**
     * Xpath to element/attribute in ARCLib XML.
     */
    private String xpath;
    /**
     * false if the whole element and all its descendants has to be indexed as text,
     * true if only element text or attribute value has to be indexed.
     */
    private boolean simple;


    public IndexFieldConfig(String fieldName, String fieldType, String xpath, boolean simple) {
        this.fieldName = fieldName;
        this.xpath = xpath;
        this.simple = simple;
        this.fieldType = null;
        for (FieldType value : FieldType.values()) {
            if (value.toString().equals(fieldType.toUpperCase().trim()))
                this.fieldType = value;
        }
        if (this.fieldType == null)
            this.fieldType = FieldType.OTHER;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexFieldConfig that = (IndexFieldConfig) o;

        return getFieldName().equals(that.getFieldName());
    }

    @Override
    public int hashCode() {
        return getFieldName().hashCode();
    }
}
