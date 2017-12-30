package cz.inqool.uas.api;

import cz.inqool.uas.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeneralEntity extends DatedObject {
    protected String stringAtt;
}
