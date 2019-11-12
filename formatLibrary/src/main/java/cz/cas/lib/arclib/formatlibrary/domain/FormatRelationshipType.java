package cz.cas.lib.arclib.formatlibrary.domain;

import lombok.Getter;

@Getter
public enum FormatRelationshipType {
    PREVIOUS_VERSION_OF("Previous version of"),
    SUBSEQUENT_VERSION_OF("Subsequent version of"),
    CAN_CONTAIN("Can contain"),
    CAN_BE_CONTAINED_BY("Can be contained by"),
    EQUIVALENT_TO("Equivalent to"),
    IS_SUBTYPE_OF("Is subtype of"),
    IS_SUPERTYPE_OF("Supertype of"),
    HAS_PRIORITY_OVER("Has priority over"),
    HAS_LOWER_PRIORITY_THAN("Has lower priority than"),
    IS_PREVIOUS_VERSION_OF("Is previous version of"),
    IS_SUBSEQUENT_VERSION_OF("Is subsequent version of"),
    OTHER("Other");

    private String label;

    FormatRelationshipType(String label) {
        this.label = label;
    }
}
