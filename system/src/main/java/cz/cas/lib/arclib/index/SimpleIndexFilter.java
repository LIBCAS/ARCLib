package cz.cas.lib.arclib.index;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class SimpleIndexFilter {
    @NonNull
    private String field;
    @NonNull
    private SimpleIndexFilterOperation operation;
    @NonNull
    private String value;
}
