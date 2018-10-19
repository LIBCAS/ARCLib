package cz.cas.lib.arclib.exception.bpm;

import java.util.Arrays;

public class ConfigParserException extends IncidentException {

    public ConfigParserException(String pathToNode, String nodeValue, Class<? extends Enum> supportedValues) {
        super("path: " + pathToNode + " value: " + nodeValue + " expected values of " + supportedValues.getSimpleName() + " enum: " + Arrays.toString(supportedValues.getEnumConstants()));
    }

    public ConfigParserException(String pathToNode, String nodeValue, String... supportedValues) {
        super("path: " + pathToNode + " value: " + nodeValue + " expected: " + Arrays.toString(supportedValues));
    }

}
