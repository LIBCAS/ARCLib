package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.index.Filter;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
public class FilterWrapper {
    @Valid
    protected List<Filter> filter = new ArrayList<>();
}
