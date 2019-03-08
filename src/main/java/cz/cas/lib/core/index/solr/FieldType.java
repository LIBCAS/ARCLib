package cz.cas.lib.core.index.solr;

/**
 * Names of field types defined in managed-schema. Do not interchange with {@link cz.cas.lib.arclib.index.FieldType}
 * <p>
 * Used by classes extending {@link SolrDomainObject} to override default field type in {@link org.springframework.data.solr.core.mapping.Indexed} annotation.
 * </p>
 */
public class FieldType {
    public static final String FOLDING = "folding";
    public static final String DATE = "pdate";
    public static final String INT = "pint";
    public static final String STRING = "string";
    public static final String TEXT = "text_general";
    public static final String DOUBLE = "pdouble";
}
