package cz.inqool.uas.index.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object for single filter condition.
 *
 * <p>
 *     Every filter condition is specified by:
 * </p>
 * <ol>
 *     <li>
 *         {@link Filter#field} representing the attribute name to filter on
 *     </li>
 *     <li>
 *         {@link Filter#operation} representing the filtering operation further described
 *         in {@link FilterOperation}
 *     </li>
 *     <li>
 *         In most cases the {@link Filter#value} to compare to
 *     </li>
 *     <li>
 *         In case of {@link FilterOperation#AND} or {@link FilterOperation#OR} also
 *         sub-filters {@link Filter#filter}
 *     </li>
 * </ol>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Filter {
    /**
     * Attribute name.
     */
    @NotNull
    protected String field;

    /**
     * Operation to do.
     */
    @NotNull
    protected FilterOperation operation;

    /**
     * Value used in comparision.
     */
    protected String value;

    /**
     * Sub-filters.
     */
    protected List<Filter> filter = new ArrayList<>();
}
