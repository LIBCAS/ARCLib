package cz.cas.lib.arclib.exception.validation;

import cz.cas.lib.core.exception.GeneralException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class InvalidXmlNodeValue extends GeneralException {
    private String expectedValue;
    private String actualValue;
    private String xPathExpression;

    @Override
    public String toString() {
        return "InvalidXmlNodeValue{" +
                "expectedValue='" + expectedValue + '\'' +
                ", actualValue='" + actualValue + '\'' +
                ", xPathExpression='" + xPathExpression + '\'' +
                '}';
    }
}
