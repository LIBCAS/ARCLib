package cz.cas.lib.arclib.formatidentifier;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface FormatIdentifier {

    /**
     * Performs the format identification analysis for all the files belonging the SIP package
     *
     * @param sipId id of the SIP to analyze
     * @return map of key-value pairs where the key is the path to a file from SIP and
     * value is a list of formats that have been identified for the respective file
     * @throws IOException if the SIP is not found
     * @throws InterruptedException
     */
    Map<String, List<String>> analyze(String sipId) throws IOException, InterruptedException;
}
