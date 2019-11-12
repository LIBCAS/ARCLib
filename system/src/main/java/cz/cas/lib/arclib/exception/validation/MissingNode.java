package cz.cas.lib.arclib.exception.validation;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;

public class MissingNode extends GeneralException {
    private String xPath;

    public MissingNode(String xPath) {
        super();
        this.xPath = xPath;
    }

    @Override
    public String toString() {
        return "MissingNode{" +
                "xpath=" + xPath +
                '}';
    }

    public String getxPath() {
        return xPath;
    }
}
