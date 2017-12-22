package cz.inqool.uas.index.dto;

/**
 * Filter operation to do.
 *
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
     *     Applicable to number or date attributes.
     * </p>
     */
    GT,

    /**
     * Less than.
     *
     * <p>
     *     Applicable to number or date attributes.
     * </p>
     */
    LT,

    /**
     * Greater than or equals.
     *
     * <p>
     *     Applicable to number or date attributes.
     * </p>
     */
    GTE,

    /**
     * Less than or equals.
     *
     * <p>
     *     Applicable to number or date attributes.
     * </p>
     */
    LTE,

    /**
     * Starts with.
     *
     * <p>
     *     Applicable to string attributes.
     * </p>
     */
    STARTWITH,

    /**
     * Ends with.
     *
     * <p>
     *     Applicable to string attributes.
     * </p>
     */
    ENDWITH,

    /**
     * Contains.
     *
     * <p>
     *     Applicable to string attributes.
     * </p>
     */
    CONTAINS,

    /**
     * Logical AND.
     *
     * <p>
     *     Applicable to sub-filters.
     * </p>
     */
    AND,

    /**
     * Logical OR.
     *
     * <p>
     *     Applicable to sub-filters.
     * </p>
     */
    OR,

    /**
     * Is not set
     *
     * <p>
     *     Applicable to all attributes.
     * </p>
     */
    IS_NULL,

    /**
     * Is set
     *
     * <p>
     *     Applicable to all attributes.
     * </p>
     */
    NOT_NULL,

    /**
     * Work with nested objects
     *
     * <p>
     *     Applicable to nested objects.
     * </p>
     */
    NESTED,

    /**
     * Negates inner filter
     */
    NEGATE
};
