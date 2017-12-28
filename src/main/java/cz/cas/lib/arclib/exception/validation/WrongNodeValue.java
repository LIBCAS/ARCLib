package cz.cas.lib.arclib.exception.validation;

import cz.inqool.uas.exception.GeneralException;

public class WrongNodeValue extends GeneralException {
    private String expectedValue;
    private String actualValue;
    private String filePath;
    private String expression;

    public WrongNodeValue(String expectedValue, String actualValue, String filePath, String expression) {
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
        this.filePath = filePath;
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "WrongNodeValue{" +
                "expectedValue='" + expectedValue + '\'' +
                ", actualValue='" + actualValue + '\'' +
                ", filePath='" + filePath + '\'' +
                ", expression='" + expression + '\'' +
                '}';
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public String getActualValue() {
        return actualValue;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getExpression() {
        return expression;
    }
}
