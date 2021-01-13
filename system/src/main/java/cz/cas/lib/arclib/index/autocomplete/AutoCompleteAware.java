package cz.cas.lib.arclib.index.autocomplete;

/**
 * Represents an entity on which the autocomplete search is supported.
 */
public interface AutoCompleteAware {

    /**
     * Returns the ID of this entity.
     */
    String getId();

    /**
     * Returns the autocomplete label value of this entity. If no value is available, returns "undefined".
     */
    String getAutoCompleteLabel();

}
