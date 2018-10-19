package cz.cas.lib.core.api;

import cz.cas.lib.core.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeneralEntity extends DatedObject {
    protected String stringAtt;
}
