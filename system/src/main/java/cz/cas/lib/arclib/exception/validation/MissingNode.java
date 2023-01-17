package cz.cas.lib.arclib.exception.validation;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;

public class MissingNode extends GeneralException {
    private String xPath;
    private String systemValidationItemCode;

    public MissingNode(String xPath) {
        super();
        this.xPath = xPath;
    }

    public MissingNode(String xPath, String systemValidationItemCode) {
        super();
        this.xPath = xPath;
        this.systemValidationItemCode = systemValidationItemCode;
    }

    @Override
    public String toString() {
        return "MissingNode{xPath=" + xPath +
                (systemValidationItemCode == null
                        ? ""
                        : ", systemValidationItemCode=" + systemValidationItemCode)
                + "}";
    }

    public String getxPath() {
        return xPath;
    }
}
