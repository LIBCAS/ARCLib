package cz.cas.lib.arclib.config;

import net.sf.saxon.Configuration;
import net.sf.saxon.jaxp.SaxonTransformerFactory;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.trans.ConfigurationReader;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.stream.StreamSource;
import java.io.File;

/**
 * This class is workaround of  Not supported: http://javax.xml.XMLConstants/property/accessExternalDTD
 * which is thrown by {@link org.springframework.xml.transform.TransformerFactoryUtils} used {@link org.springframework.ws.server.endpoint.mapping.PayloadRootAnnotationMethodEndpointMapping}
 * in spring-ws. It tries o set configuration properties which Saxon does not know.
 * <p>
 * This class catch configuration exception and logs it logs it.
 */
public class ArclibTransformerFactory extends SaxonTransformerFactory {

    private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ArclibTransformerFactory.class);


    public void setAttribute(String name, Object value) throws IllegalArgumentException {
        switch (name) {
            case FeatureKeys.CONFIGURATION:
                setConfiguration((Configuration) value);
                break;
            case FeatureKeys.CONFIGURATION_FILE:
                ConfigurationReader reader = new ConfigurationReader();
                try {
                    setConfiguration(reader.makeConfiguration(new StreamSource(new File((String) value))));
                } catch (XPathException err) {
                    throw new IllegalArgumentException(err);
                }
                break;
            default:
                try {
                    getProcessor().getUnderlyingConfiguration().setConfigurationProperty(name, value);
                } catch (IllegalArgumentException e) {
                    log.warn(e.toString());
                }
                break;
        }
    }
}
