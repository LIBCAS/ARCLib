package cz.cas.lib.arclib.validation.exception;

import cz.inqool.uas.exception.GeneralException;

public class MissingFile extends GeneralException {
    private String filePath;
    private String validationProfileId;

    public MissingFile(String filePath, String validationProfileId) {
        this.filePath = filePath;
        this.validationProfileId = validationProfileId;
    }

    @Override
    public String toString() {
        return "MissingFile{" +
                "filePath=" + filePath +
                "validationProfileId" + validationProfileId + '\'' +
                '}';
    }

    public String getFilePath() {
        return filePath;
    }

    public String getValidationProfileId() {
        return validationProfileId;
    }
}
