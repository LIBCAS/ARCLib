package cz.cas.lib.arclib.exception.validation;

import cz.inqool.uas.exception.GeneralException;

public class InvalidNodeValue extends GeneralException {
    private String regex;
    private String actualValue;
    private String filePath;
    private String xPathExpression;

    public InvalidNodeValue(String regex, String actualValue, String filePath, String xPathExpression) {
        this.regex = regex;
        this.actualValue = actualValue;
        this.filePath = filePath;
        this.xPathExpression = xPathExpression;
    }

    @Override
    public String toString() {
        return "InvalidNodeValue{" +
                "regex='" + regex + '\'' +
                ", actualValue='" + actualValue + '\'' +
                ", filePath='" + filePath + '\'' +
                ", xPathExpression='" + xPathExpression + '\'' +
                '}';
    }

    public String getRegex() {
        return regex;
    }

    public String getActualValue() {
        return actualValue;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getxPathExpression() {
        return xPathExpression;
    }
}
