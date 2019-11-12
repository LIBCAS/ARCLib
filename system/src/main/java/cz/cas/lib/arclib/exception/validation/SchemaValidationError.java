package cz.cas.lib.arclib.exception.validation;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SchemaValidationError extends GeneralException {
    private String sipId;
    private String validationProfileId;
    private String xmlPath;
    private String xsdSchema;
    private String message;

    @Override
    public String toString() {
        return "SchemaValidationError{" +
                "sipId='" + sipId + '\'' +
                ", validationProfileId=' \n" + validationProfileId + '\'' +
                ", xmlPath=' \n" + xmlPath + '\'' +
                ", xsdSchema=' \n" + xsdSchema + '\'' +
                ", message=' \n" + message + '\'' +
                '}';
    }
}
