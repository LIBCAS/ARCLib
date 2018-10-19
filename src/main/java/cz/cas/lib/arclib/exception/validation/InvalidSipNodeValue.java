package cz.cas.lib.arclib.exception.validation;

import cz.cas.lib.core.exception.GeneralException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class InvalidSipNodeValue extends GeneralException {
    private String sipId;
    private String validationProfileId;
    private String regex;
    private String actualValue;
    private String filePath;
    private String xPathExpression;

    @Override
    public String toString() {
        return "InvalidNodeValue{" +
                "sipId='" + sipId + '\'' +
                ", validationProfileId='" + validationProfileId + '\'' +
                ", regex='" + regex + '\'' +
                ", actualValue='" + actualValue + '\'' +
                ", filePath='" + filePath + '\'' +
                ", xPathExpression='" + xPathExpression + '\'' +
                '}';
    }
}
