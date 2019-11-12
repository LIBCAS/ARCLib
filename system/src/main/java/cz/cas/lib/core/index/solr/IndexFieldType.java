package cz.cas.lib.core.index.solr;

/**
 * Names of field types defined in managed-schema.
 * <p>
 * Used by classes extending {@link IndexedDomainObject} to override default field type in {@link org.springframework.data.solr.core.mapping.Indexed} annotation.
 * </p>
 * <p>
 * For one collection there can be only one type per field name.
 * For example, when object A and B are both indexed under collection C and object A has a field <i>name</i> of type <i>folding</i>, than object B can't have field <i>name</i> of type <i>string</i>.
 * </p>
 */
public class IndexFieldType {
    public static final String BOOLEAN = "boolean";
    public static final String DATE = "pdate";
    public static final String STRING = "string";
    /**
     * keyword with ascii folding and lowercase
     */
    public static final String FOLDING = "keyword_folding";
    /**
     * standard tokenizer with ascii folding and lowercase
     * should be used for longer texts with some advanced analysis (stopwords etc.) or when e.g. proximity search is required
     */
    public static final String TEXT = "standard_folding";
    public static final String INT = "pint";
    public static final String LONG = "plong";
    public static final String DOUBLE = "pdouble";
}
