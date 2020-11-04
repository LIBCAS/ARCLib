package cz.cas.lib.arclib.exception.validation;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;

public class MultipleNodesFound extends GeneralException {
    private String xPath;

    public MultipleNodesFound(String xPath) {
        super();
        this.xPath = xPath;
    }

    @Override
    public String toString() {
        return "MultipleNodesFound{" +
                "xpath=" + xPath +
                '}';
    }

    public String getxPath() {
        return xPath;
    }
}
