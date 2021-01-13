package cz.cas.lib.arclib.index.autocomplete;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Class representing the autocomplete search item
 */
@Getter
@Setter
@NoArgsConstructor
public final class AutoCompleteItem {
    private String id;
    private String autoCompleteLabel;
}
