package cz.cas.lib.core.index.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Data transfer object for specification of sorting.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SortSpecification implements Serializable {
    /**
     * Attribute name to sort on.
     */
    @NotNull
    protected String sort = "created";

    /**
     * Order of sorting.
     *
     * <p>
     * For possible values see {@link Order}.
     * </p>
     */
    @NotNull
    protected Order order = Order.DESC;
}
