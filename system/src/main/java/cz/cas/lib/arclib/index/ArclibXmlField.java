package cz.cas.lib.arclib.index;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration class of the index field used during index document creation.
 */
@Getter
@Setter
@AllArgsConstructor
public class ArclibXmlField {

    /**
     * name of the field in index
     */
    private String fieldName;
    /**
     * some values of AIP XML nodes need special treatment (e.g. DATE values)
     */
    private AipXmlNodeValueType aipXmlNodeValueType;
    /**
     * Xpath to element/attribute in ARCLib XML. If this is field of some child index type, than the xpath is relative to {@link ArclibXmlIndexTypeConfig#rootXpath}
     */
    private String xpath;
    /**
     * false if the whole element and all its descendants has to be indexed as text,
     * true if only element text or attribute value has to be indexed.
     */
    private boolean simple;


    public ArclibXmlField(String fieldName, String aipXmlNodeValueType, String xpath, boolean simple) {
        this.fieldName = fieldName;
        this.xpath = xpath;
        this.simple = simple;
        this.aipXmlNodeValueType = null;
        for (AipXmlNodeValueType value : AipXmlNodeValueType.values()) {
            if (value.toString().equals(aipXmlNodeValueType.toUpperCase().trim()))
                this.aipXmlNodeValueType = value;
        }
        if (this.aipXmlNodeValueType == null)
            this.aipXmlNodeValueType = AipXmlNodeValueType.OTHER;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArclibXmlField that = (ArclibXmlField) o;

        return getFieldName().equals(that.getFieldName());
    }

    @Override
    public int hashCode() {
        return getFieldName().hashCode();
    }
}
