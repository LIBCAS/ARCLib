package cz.cas.lib.arclib.exception.validation;

import cz.inqool.uas.exception.GeneralException;

public class SchemaValidationError extends GeneralException {
    private String xmlPath;
    private String xsdSchema;
    private String message;

    public SchemaValidationError(String xmlPath, String xsdSchema, String message) {
        this.xmlPath = xmlPath;
        this.xsdSchema = xsdSchema;
        this.message = message;
    }

    @Override
    public String toString() {
        return "SchemaValidationError{" +
                "xmlPath='" + xmlPath + '\'' +
                ", xsdSchema=' \n" + xsdSchema + '\'' +
                ", message=' \n" + message + '\'' +
                '}';
    }

    public String getXmlPath() {
        return xmlPath;
    }

    public String getXsdSchema() {
        return xsdSchema;
    }

    public String getMessage() {
        return message;
    }
}
