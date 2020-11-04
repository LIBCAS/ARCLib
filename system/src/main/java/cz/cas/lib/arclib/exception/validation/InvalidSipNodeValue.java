package cz.cas.lib.arclib.exception.validation;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class InvalidSipNodeValue extends GeneralException {
    private String sipId;
    private String validationProfileExternalId;
    private String regex;
    private String actualValue;
    private String filePath;
    private String xPathExpression;

    @Override
    public String toString() {
        return "InvalidNodeValue{" +
                "sipId='" + sipId + '\'' +
                ", validationProfileExternalId='" + validationProfileExternalId + '\'' +
                ", regex='" + regex + '\'' +
                ", actualValue='" + actualValue + '\'' +
                ", filePath='" + filePath + '\'' +
                ", xPathExpression='" + xPathExpression + '\'' +
                '}';
    }
}
