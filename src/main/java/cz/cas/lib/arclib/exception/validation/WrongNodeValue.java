package cz.cas.lib.arclib.exception.validation;

import cz.cas.lib.core.exception.GeneralException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class WrongNodeValue extends GeneralException {
    private String sipId;
    private String validationProfileId;
    private String expectedValue;
    private String actualValue;
    private String filePath;
    private String expression;

    @Override
    public String toString() {
        return "WrongNodeValue{" +
                "sipId='" + sipId + '\'' +
                ", validationProfileId='" + validationProfileId + '\'' +
                ", expectedValue='" + expectedValue + '\'' +
                ", actualValue='" + actualValue + '\'' +
                ", filePath='" + filePath + '\'' +
                ", expression='" + expression + '\'' +
                '}';
    }
}
