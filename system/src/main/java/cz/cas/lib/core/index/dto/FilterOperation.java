package cz.cas.lib.core.index.dto;

import cz.cas.lib.arclib.index.solr.IndexQueryUtils;

/**
 * Filter operation to do.
 */
public enum FilterOperation {
    /**
     * Equal.
     */
    EQ,

    /**
     * Not equal.
     */
    NEQ,

    /**
     * Greater than.
     *
     * <p>
     * Applicable to number or date attributes.
     * </p>
     */
    GT,

    /**
     * Less than.
     *
     * <p>
     * Applicable to number or date attributes.
     * </p>
     */
    LT,

    /**
     * Greater than or equals.
     *
     * <p>
     * Applicable to number or date attributes.
     * </p>
     */
    GTE,

    /**
     * Less than or equals.
     *
     * <p>
     * Applicable to number or date attributes.
     * </p>
     */
    LTE,

    /**
     * Starts with.
     *
     * <p>
     * Applicable to string attributes.
     * </p>
     */
    STARTWITH,

    /**
     * Ends with.
     *
     * <p>
     * Applicable to string attributes.
     * </p>
     */
    ENDWITH,

    /**
     * Contains.
     *
     * <p>
     * Applicable to string attributes.
     * </p>
     */
    CONTAINS,

    /**
     * Logical AND.
     *
     * <p>
     * Applicable to sub-filters.
     * </p>
     */
    AND,

    /**
     * Logical OR.
     *
     * <p>
     * Applicable to sub-filters.
     * </p>
     */
    OR,

    /**
     * Is not set
     *
     * <p>
     * Applicable to all attributes.
     * </p>
     */
    IS_NULL,

    /**
     * Is set
     *
     * <p>
     * Applicable to all attributes.
     * </p>
     */
    NOT_NULL,

    /**
     * Work with nested objects
     *
     * <p>
     * Used to filter parents which has at least one child matching nested {@link Filter#filter} condition.
     * </p>
     * <p>
     * Parent and child entities must be in the same Solr collection. {@link Filter#value} must be set to index type which
     * exists as key in {@link IndexQueryUtils#INDEXED_FIELDS_MAP}. {@link Filter#filter} contains
     * filters which are executed on child collection.
     * </p>
     */
    NESTED,

    /**
     * Negates inner filter
     */
    NEGATE,
    /**
     * IN(v1,v2) is a shortcut for (EQ=v1 OR EQ=v2)
     */
    IN
};
