package cz.cas.lib.arclib.index;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Arclib XML index consists of parent doc {@link IndexArclibXmlStore#getMainDocumentIndexType()} containing fields for the whole
 * Arclib XML e.g. AIP STATE and also child index types - nested indexes e.g. identified_format.
 * <p>
 * This class contains configuration of parent or one of its child. The configuration is used during indexation so that
 * system knows where to look for elements in AIP XML and how to index them.
 * </p>
 * <p>
 * Note that not all elements of the parent index type are contained in AIP XML, e.g. {@link cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument#PRODUCER_NAME}
 * is not.
 * </p>
 */
@Getter
@Setter
public class ArclibXmlIndexTypeConfig {
    /**
     * used only by children index types, every field of the child contains xpath relative to this
     */
    private String rootXpath;
    /**
     * name of the parent index type or particular child index type
     */
    private String indexType;
    /**
     * configuration of fields of the index type
     */
    private Set<ArclibXmlField> IndexedFieldConfig = new HashSet<>();

    public ArclibXmlIndexTypeConfig(String rootXpath, String indexType) {
        this.rootXpath = rootXpath;
        this.indexType = indexType;
    }
}
