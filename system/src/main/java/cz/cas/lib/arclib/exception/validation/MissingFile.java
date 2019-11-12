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
    private String validationProfileId;
    private String filePath;

    @Override
    public String toString() {
        return "MissingFile{" +
                "sipId='" + sipId + '\'' +
                ", validationProfileId='" + validationProfileId + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}
