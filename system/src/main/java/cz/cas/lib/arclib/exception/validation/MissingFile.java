package cz.cas.lib.arclib.exception.validation;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MissingFile extends GeneralException {
    private String sipId;
    private String validationProfileExternalId;
    private String filePath;

    @Override
    public String toString() {
        return "MissingFile{" +
                "sipId='" + sipId + '\'' +
                ", validationProfileExternalId='" + validationProfileExternalId + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}
